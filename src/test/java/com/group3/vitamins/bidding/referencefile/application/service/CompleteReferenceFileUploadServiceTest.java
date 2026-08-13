package com.group3.vitamins.bidding.referencefile.application.service;

import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.bidding.referencefile.application.command.CompleteReferenceFileUploadCommand;
import com.group3.vitamins.bidding.referencefile.application.result.CompleteReferenceFileUploadResult;
import com.group3.vitamins.bidding.referencefile.domain.exception.BidReferenceFileErrorCode;
import com.group3.vitamins.bidding.referencefile.domain.model.BidReferenceFile;
import com.group3.vitamins.bidding.referencefile.domain.model.ReferenceFileIndexStatus;
import com.group3.vitamins.bidding.referencefile.domain.model.ReferenceFileUploadStatus;
import com.group3.vitamins.bidding.referencefile.domain.repository.BidReferenceFileRepository;
import com.group3.vitamins.file.application.port.FileStoragePort;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("CompleteReferenceFileUploadService 입찰 기준자료 업로드 완료")
class CompleteReferenceFileUploadServiceTest {

    private static final Long COMPANY_ID = 10L;
    private static final Long REFERENCE_FILE_ID = 501L;
    private static final String USER_ID = "EMP001";
    private static final String ROLE = "MEMBER";
    private static final String STORAGE_KEY = "companies/10/bidding/reference-files/uuid.pdf";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 12, 10, 0);

    private BidReferenceFileRepository referenceFileRepository;
    private BidReferenceFileFailureRecorder failureRecorder;
    private FileStoragePort fileStoragePort;
    private BiddingAccessPolicy biddingAccessPolicy;
    private CompleteReferenceFileUploadService service;

    @BeforeEach
    void setUp() {
        referenceFileRepository = mock(BidReferenceFileRepository.class);
        failureRecorder = mock(BidReferenceFileFailureRecorder.class);
        fileStoragePort = mock(FileStoragePort.class);
        biddingAccessPolicy = mock(BiddingAccessPolicy.class);
        CurrentCompanyIdProvider companyIdProvider = mock(CurrentCompanyIdProvider.class);
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-12T01:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );

        service = new CompleteReferenceFileUploadService(
                referenceFileRepository, failureRecorder, fileStoragePort,
                biddingAccessPolicy, companyIdProvider, clock
        );

        when(companyIdProvider.currentCompanyId()).thenReturn(COMPANY_ID);
        when(referenceFileRepository.findByIdAndCompanyId(REFERENCE_FILE_ID, COMPANY_ID))
                .thenReturn(Optional.of(uploadingFile()));
        when(referenceFileRepository.saveCompletedWithIndexOutbox(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("저장소에 크기가 일치하는 객체가 있으면 COMPLETED로 전이하고 인덱싱 Outbox를 적재한다")
    void completesSuccessfully() {
        when(fileStoragePort.head(STORAGE_KEY))
                .thenReturn(Optional.of(new FileStoragePort.StoredObject(204800L)));

        CompleteReferenceFileUploadResult result = service.complete(
                new CompleteReferenceFileUploadCommand(REFERENCE_FILE_ID, USER_ID, ROLE)
        );

        assertThat(result.referenceFileId()).isEqualTo(REFERENCE_FILE_ID);
        assertThat(result.uploadStatus()).isEqualTo("COMPLETED");
        assertThat(result.indexStatus()).isEqualTo("PENDING");
        verify(referenceFileRepository).saveCompletedWithIndexOutbox(any());
        verifyNoInteractions(failureRecorder);
    }

    @Test
    @DisplayName("저장소에 객체가 없으면 409로 거절하고 실패 상태를 별도 트랜잭션으로 기록한다")
    void rejectsWhenObjectMissing() {
        when(fileStoragePort.head(STORAGE_KEY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.complete(
                new CompleteReferenceFileUploadCommand(REFERENCE_FILE_ID, USER_ID, ROLE)
        ))
                .isInstanceOf(ConflictException.class)
                .satisfies(exception -> assertThat(((ConflictException) exception).getErrorCode())
                        .isEqualTo(BidReferenceFileErrorCode.BIDDING_REFERENCE_FILE_OBJECT_NOT_FOUND));

        verify(failureRecorder).markUploadFailed(any(), any());
        verify(referenceFileRepository, never()).saveCompletedWithIndexOutbox(any());
    }

    @Test
    @DisplayName("업로드된 크기가 신고한 크기와 다르면 409로 거절하고 실패 상태를 기록한다")
    void rejectsWhenSizeMismatches() {
        when(fileStoragePort.head(STORAGE_KEY))
                .thenReturn(Optional.of(new FileStoragePort.StoredObject(1L)));

        assertThatThrownBy(() -> service.complete(
                new CompleteReferenceFileUploadCommand(REFERENCE_FILE_ID, USER_ID, ROLE)
        ))
                .isInstanceOf(ConflictException.class)
                .satisfies(exception -> assertThat(((ConflictException) exception).getErrorCode())
                        .isEqualTo(BidReferenceFileErrorCode.BIDDING_REFERENCE_FILE_SIZE_MISMATCH));

        verify(failureRecorder).markUploadFailed(any(), any());
        verify(referenceFileRepository, never()).saveCompletedWithIndexOutbox(any());
    }

    @Test
    @DisplayName("존재하지 않거나 다른 회사의 기준자료면 404를 던진다")
    void rejectsWhenNotFound() {
        when(referenceFileRepository.findByIdAndCompanyId(REFERENCE_FILE_ID, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.complete(
                new CompleteReferenceFileUploadCommand(REFERENCE_FILE_ID, USER_ID, ROLE)
        ))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(fileStoragePort, failureRecorder);
    }

    @Test
    @DisplayName("이미 완료·실패된 기준자료는 다시 완료 처리할 수 없다")
    void rejectsWhenNotUploading() {
        when(referenceFileRepository.findByIdAndCompanyId(REFERENCE_FILE_ID, COMPANY_ID))
                .thenReturn(Optional.of(completedFile()));

        assertThatThrownBy(() -> service.complete(
                new CompleteReferenceFileUploadCommand(REFERENCE_FILE_ID, USER_ID, ROLE)
        ))
                .isInstanceOf(ConflictException.class)
                .satisfies(exception -> assertThat(((ConflictException) exception).getErrorCode())
                        .isEqualTo(BidReferenceFileErrorCode.BIDDING_REFERENCE_FILE_NOT_UPLOADING));

        verifyNoInteractions(fileStoragePort, failureRecorder);
    }

    @Test
    @DisplayName("presigned 업로드 URL이 만료된 뒤 완료 호출하면 실패로 기록하고 거절한다")
    void rejectsWhenUploadExpired() {
        when(referenceFileRepository.findByIdAndCompanyId(REFERENCE_FILE_ID, COMPANY_ID))
                .thenReturn(Optional.of(expiredUploadFile()));

        assertThatThrownBy(() -> service.complete(
                new CompleteReferenceFileUploadCommand(REFERENCE_FILE_ID, USER_ID, ROLE)
        ))
                .isInstanceOf(ConflictException.class)
                .satisfies(exception -> assertThat(((ConflictException) exception).getErrorCode())
                        .isEqualTo(BidReferenceFileErrorCode.BIDDING_REFERENCE_FILE_UPLOAD_EXPIRED));

        verify(failureRecorder).markUploadFailed(any(), any());
        verifyNoInteractions(fileStoragePort);
    }

    @Test
    @DisplayName("입찰 관리 권한이 없으면 저장소를 건드리기 전에 403으로 막힌다")
    void shortCircuitsOnAccessDenied() {
        doThrow(new ForbiddenException(BiddingErrorCode.BIDDING_ACCESS_PERMISSION_REQUIRED))
                .when(biddingAccessPolicy).assertAccess(USER_ID, ROLE);

        assertThatThrownBy(() -> service.complete(
                new CompleteReferenceFileUploadCommand(REFERENCE_FILE_ID, USER_ID, ROLE)
        )).isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(referenceFileRepository, fileStoragePort, failureRecorder);
    }

    private BidReferenceFile uploadingFile() {
        return new BidReferenceFile(
                REFERENCE_FILE_ID, COMPANY_ID, "원가계산_기준.pdf", "pdf", "application/pdf",
                204800L, STORAGE_KEY, ReferenceFileUploadStatus.UPLOADING, ReferenceFileIndexStatus.PENDING,
                null, 0, null, NOW.plusMinutes(10), null, null, USER_ID, NOW, NOW, null
        );
    }

    private BidReferenceFile completedFile() {
        return new BidReferenceFile(
                REFERENCE_FILE_ID, COMPANY_ID, "원가계산_기준.pdf", "pdf", "application/pdf",
                204800L, STORAGE_KEY, ReferenceFileUploadStatus.COMPLETED, ReferenceFileIndexStatus.COMPLETED,
                "attempt-1", 0, null, NOW.plusMinutes(10), NOW, NOW, USER_ID, NOW, NOW, null
        );
    }

    private BidReferenceFile expiredUploadFile() {
        return new BidReferenceFile(
                REFERENCE_FILE_ID, COMPANY_ID, "원가계산_기준.pdf", "pdf", "application/pdf",
                204800L, STORAGE_KEY, ReferenceFileUploadStatus.UPLOADING, ReferenceFileIndexStatus.PENDING,
                null, 0, null, NOW.minusMinutes(1), null, null, USER_ID, NOW, NOW, null
        );
    }
}
