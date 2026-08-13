package com.group3.vitamins.bidding.referencefile.application.service;

import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.bidding.referencefile.application.command.StartReferenceFileUploadCommand;
import com.group3.vitamins.bidding.referencefile.application.result.StartReferenceFileUploadResult;
import com.group3.vitamins.bidding.referencefile.domain.exception.BidReferenceFileErrorCode;
import com.group3.vitamins.bidding.referencefile.domain.model.BidReferenceFile;
import com.group3.vitamins.bidding.referencefile.domain.repository.BidReferenceFileRepository;
import com.group3.vitamins.file.application.port.FileStoragePort;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("StartReferenceFileUploadService 입찰 기준자료 업로드 시작")
class StartReferenceFileUploadServiceTest {

    private static final Long COMPANY_ID = 10L;
    private static final String USER_ID = "EMP001";
    private static final String ROLE = "MEMBER";

    private BidReferenceFileRepository referenceFileRepository;
    private FileStoragePort fileStoragePort;
    private BiddingAccessPolicy biddingAccessPolicy;
    private StartReferenceFileUploadService service;

    @BeforeEach
    void setUp() {
        referenceFileRepository = mock(BidReferenceFileRepository.class);
        fileStoragePort = mock(FileStoragePort.class);
        biddingAccessPolicy = mock(BiddingAccessPolicy.class);
        CurrentCompanyIdProvider companyIdProvider = mock(CurrentCompanyIdProvider.class);
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-12T01:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );

        service = new StartReferenceFileUploadService(
                referenceFileRepository, fileStoragePort, biddingAccessPolicy, companyIdProvider, clock
        );

        when(companyIdProvider.currentCompanyId()).thenReturn(COMPANY_ID);
        when(referenceFileRepository.save(any()))
                .thenAnswer(invocation -> withId(invocation.getArgument(0), 501L));
        when(fileStoragePort.presignUpload(any(), any(), anyLong()))
                .thenReturn(new FileStoragePort.PresignedUrl(
                        "https://s3.example.com/presigned-put",
                        Instant.parse("2026-08-12T01:10:00Z")
                ));
    }

    @Test
    @DisplayName("정상 요청이면 UPLOADING 상태로 저장하고 presigned 업로드 URL을 발급한다")
    void startsUploadSuccessfully() {
        StartReferenceFileUploadCommand command = new StartReferenceFileUploadCommand(
                "원가계산_기준.pdf", "application/pdf", 204800L, USER_ID, ROLE
        );

        StartReferenceFileUploadResult result = service.start(command);

        assertThat(result.referenceFileId()).isEqualTo(501L);
        assertThat(result.uploadUrl()).isEqualTo("https://s3.example.com/presigned-put");

        ArgumentCaptor<BidReferenceFile> captor = ArgumentCaptor.forClass(BidReferenceFile.class);
        verify(referenceFileRepository).save(captor.capture());
        BidReferenceFile saved = captor.getValue();
        assertThat(saved.companyId()).isEqualTo(COMPANY_ID);
        assertThat(saved.fileName()).isEqualTo("원가계산_기준.pdf");
        assertThat(saved.extension()).isEqualTo("pdf");
        assertThat(saved.storageKey()).startsWith("companies/10/bidding/reference-files/");
        assertThat(saved.storageKey()).endsWith(".pdf");
        assertThat(saved.createdBy()).isEqualTo(USER_ID);

        verify(fileStoragePort).presignUpload(saved.storageKey(), "application/pdf", 204800L);
    }

    @Test
    @DisplayName("파일명이 비어 있으면 400으로 거절하고 저장소를 건드리지 않는다")
    void rejectsBlankFileName() {
        StartReferenceFileUploadCommand command = new StartReferenceFileUploadCommand(
                "   ", "application/pdf", 1000L, USER_ID, ROLE
        );

        assertThatThrownBy(() -> service.start(command))
                .isInstanceOf(ValidationException.class)
                .satisfies(exception -> assertThat(((ValidationException) exception).getErrorCode())
                        .isEqualTo(BidReferenceFileErrorCode.BIDDING_INVALID_REFERENCE_FILE_REQUEST));

        verifyNoInteractions(referenceFileRepository, fileStoragePort);
    }

    @Test
    @DisplayName("파일 크기가 50MB를 넘으면 400으로 거절한다")
    void rejectsOversizedFile() {
        StartReferenceFileUploadCommand command = new StartReferenceFileUploadCommand(
                "big.pdf", "application/pdf", 52428801L, USER_ID, ROLE
        );

        assertThatThrownBy(() -> service.start(command))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(referenceFileRepository, fileStoragePort);
    }

    @Test
    @DisplayName("파일 크기가 0 이하이면 400으로 거절한다")
    void rejectsNonPositiveSize() {
        StartReferenceFileUploadCommand command = new StartReferenceFileUploadCommand(
                "empty.pdf", "application/pdf", 0L, USER_ID, ROLE
        );

        assertThatThrownBy(() -> service.start(command))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(referenceFileRepository, fileStoragePort);
    }

    @Test
    @DisplayName("입찰 관리 권한이 없으면 저장소를 건드리기 전에 403으로 막힌다")
    void shortCircuitsOnAccessDenied() {
        doThrow(new ForbiddenException(BiddingErrorCode.BIDDING_ACCESS_PERMISSION_REQUIRED))
                .when(biddingAccessPolicy).assertAccess(USER_ID, ROLE);

        StartReferenceFileUploadCommand command = new StartReferenceFileUploadCommand(
                "원가계산_기준.pdf", "application/pdf", 204800L, USER_ID, ROLE
        );

        assertThatThrownBy(() -> service.start(command))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(referenceFileRepository, fileStoragePort);
    }

    private BidReferenceFile withId(BidReferenceFile file, Long id) {
        return new BidReferenceFile(
                id, file.companyId(), file.fileName(), file.extension(), file.mimeType(),
                file.sizeBytes(), file.storageKey(), file.uploadStatus(), file.indexStatus(),
                file.indexAttemptId(), file.indexRetryCount(), file.indexErrorMessage(),
                file.uploadExpiresAt(), file.completedAt(), file.indexedAt(), file.createdBy(),
                file.createdAt(), file.updatedAt(), file.deletedAt()
        );
    }
}
