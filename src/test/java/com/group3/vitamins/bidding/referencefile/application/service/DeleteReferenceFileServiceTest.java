package com.group3.vitamins.bidding.referencefile.application.service;

import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.bidding.referencefile.application.command.DeleteReferenceFileCommand;
import com.group3.vitamins.bidding.referencefile.application.port.BidReferenceFileActiveUsagePort;
import com.group3.vitamins.bidding.referencefile.domain.exception.BidReferenceFileErrorCode;
import com.group3.vitamins.bidding.referencefile.domain.model.BidReferenceFile;
import com.group3.vitamins.bidding.referencefile.domain.model.ReferenceFileIndexStatus;
import com.group3.vitamins.bidding.referencefile.domain.model.ReferenceFileUploadStatus;
import com.group3.vitamins.bidding.referencefile.domain.repository.BidReferenceFileRepository;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("DeleteReferenceFileService 입찰 기준자료 삭제")
class DeleteReferenceFileServiceTest {

    private static final Long COMPANY_ID = 10L;
    private static final Long REFERENCE_FILE_ID = 501L;
    private static final String USER_ID = "EMP001";
    private static final String ROLE = "MEMBER";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 12, 10, 0);

    private BidReferenceFileRepository referenceFileRepository;
    private BidReferenceFileActiveUsagePort activeUsagePort;
    private BiddingAccessPolicy biddingAccessPolicy;
    private DeleteReferenceFileService service;

    @BeforeEach
    void setUp() {
        referenceFileRepository = mock(BidReferenceFileRepository.class);
        activeUsagePort = mock(BidReferenceFileActiveUsagePort.class);
        biddingAccessPolicy = mock(BiddingAccessPolicy.class);
        CurrentCompanyIdProvider companyIdProvider = mock(CurrentCompanyIdProvider.class);
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-12T01:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );

        service = new DeleteReferenceFileService(
                referenceFileRepository, activeUsagePort, biddingAccessPolicy, companyIdProvider, clock
        );

        when(companyIdProvider.currentCompanyId()).thenReturn(COMPANY_ID);
        when(referenceFileRepository.findActiveByIdAndCompanyIdForDeletion(REFERENCE_FILE_ID, COMPANY_ID))
                .thenReturn(Optional.of(completedFile()));
    }

    @Test
    @DisplayName("사용 중이 아니면 논리 삭제와 정리 Outbox를 저장한다")
    void deletesSuccessfully() {
        when(activeUsagePort.existsActiveReviewUsage(COMPANY_ID, REFERENCE_FILE_ID))
                .thenReturn(false);

        service.delete(new DeleteReferenceFileCommand(REFERENCE_FILE_ID, USER_ID, ROLE));

        ArgumentCaptor<BidReferenceFile> captor = ArgumentCaptor.forClass(BidReferenceFile.class);
        verify(referenceFileRepository).saveDeletedWithCleanupOutbox(captor.capture());
        BidReferenceFile deleted = captor.getValue();
        assertThat(deleted.deletedAt()).isEqualTo(NOW);
        assertThat(deleted.indexAttemptId()).isEqualTo("attempt-1");
    }

    @Test
    @DisplayName("PENDING 또는 PROCESSING 검토가 사용 중이면 409로 거절한다")
    void rejectsWhenInUse() {
        when(activeUsagePort.existsActiveReviewUsage(COMPANY_ID, REFERENCE_FILE_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service.delete(
                new DeleteReferenceFileCommand(REFERENCE_FILE_ID, USER_ID, ROLE)
        ))
                .isInstanceOf(ConflictException.class)
                .satisfies(exception -> assertThat(((ConflictException) exception).getErrorCode())
                        .isEqualTo(BidReferenceFileErrorCode.BIDDING_REFERENCE_FILE_IN_USE));

        verify(referenceFileRepository, never()).saveDeletedWithCleanupOutbox(any());
    }

    @Test
    @DisplayName("존재하지 않거나 다른 회사의 기준자료면 404를 던진다")
    void rejectsWhenNotFound() {
        when(referenceFileRepository.findActiveByIdAndCompanyIdForDeletion(REFERENCE_FILE_ID, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(
                new DeleteReferenceFileCommand(REFERENCE_FILE_ID, USER_ID, ROLE)
        ))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(activeUsagePort);
        verify(referenceFileRepository, never()).saveDeletedWithCleanupOutbox(any());
    }

    @Test
    @DisplayName("입찰 관리 권한이 없으면 저장소를 건드리기 전에 403으로 막힌다")
    void shortCircuitsOnAccessDenied() {
        doThrow(new ForbiddenException(BiddingErrorCode.BIDDING_ACCESS_PERMISSION_REQUIRED))
                .when(biddingAccessPolicy).assertAccess(USER_ID, ROLE);

        assertThatThrownBy(() -> service.delete(
                new DeleteReferenceFileCommand(REFERENCE_FILE_ID, USER_ID, ROLE)
        )).isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(referenceFileRepository, activeUsagePort);
    }

    private BidReferenceFile completedFile() {
        return new BidReferenceFile(
                REFERENCE_FILE_ID, COMPANY_ID, "원가계산_기준.pdf", "pdf", "application/pdf",
                204800L, "companies/10/bidding/reference-files/uuid.pdf",
                ReferenceFileUploadStatus.COMPLETED, ReferenceFileIndexStatus.COMPLETED,
                "attempt-1", 0, null, null, NOW, NOW, USER_ID, NOW, NOW, null
        );
    }
}
