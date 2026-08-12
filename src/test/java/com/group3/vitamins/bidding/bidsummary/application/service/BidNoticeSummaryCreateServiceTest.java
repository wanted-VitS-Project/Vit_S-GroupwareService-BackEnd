package com.group3.vitamins.bidding.bidsummary.application.service;

import com.group3.vitamins.bidding.bidsummary.application.command.CreateBidNoticeSummaryCommand;
import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryCommandPort;
import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryNoticePort;
import com.group3.vitamins.bidding.bidsummary.domain.model.BidNoticeSummary;
import com.group3.vitamins.bidding.bidsummary.domain.model.BidNoticeSummaryStatus;
import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("BidNoticeSummaryCreateService 개선 요약 요청")
class BidNoticeSummaryCreateServiceTest {

    private static final Long COMPANY_ID = 10L;
    private static final Long NOTICE_ID = 20L;
    private static final Long BASE_SUMMARY_ID = 31L;
    private static final String USER_ID = "EMP001";
    private static final String ROLE = "MEMBER";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 12, 10, 0);

    private BidNoticeSummaryNoticePort noticePort;
    private BidNoticeSummaryCommandPort commandPort;
    private BidNoticeSummaryCreateService service;

    @BeforeEach
    void setUp() {
        noticePort = mock(BidNoticeSummaryNoticePort.class);
        commandPort = mock(BidNoticeSummaryCommandPort.class);
        BiddingAccessPolicy accessPolicy = mock(BiddingAccessPolicy.class);
        CurrentCompanyIdProvider companyIdProvider = mock(CurrentCompanyIdProvider.class);
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-12T01:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );
        service = new BidNoticeSummaryCreateService(
                noticePort,
                commandPort,
                accessPolicy,
                companyIdProvider,
                clock
        );

        when(companyIdProvider.currentCompanyId()).thenReturn(COMPANY_ID);
        when(noticePort.findAccessibleNotice(COMPANY_ID, NOTICE_ID))
                .thenReturn(Optional.of(mock(BidNoticeSummaryNoticePort.BidNoticeSnapshot.class)));
        when(commandPort.existsInProgress(COMPANY_ID, NOTICE_ID, USER_ID))
                .thenReturn(false);
        when(commandPort.savePendingWithOutbox(any(), any()))
                .thenAnswer(invocation -> withId(invocation.getArgument(0)));
    }

    @Test
    @DisplayName("완료된 본인 미확정 요약을 기준으로 다음 개정 요청을 생성한다")
    void createsImprovementRevision() {
        when(commandPort.findImprovementBaseForUpdate(
                COMPANY_ID, NOTICE_ID, USER_ID, BASE_SUMMARY_ID
        )).thenReturn(Optional.of(base(3, BidNoticeSummaryStatus.COMPLETED, false)));

        var result = service.create(command(BASE_SUMMARY_ID));

        ArgumentCaptor<BidNoticeSummary> captor = ArgumentCaptor.forClass(BidNoticeSummary.class);
        verify(commandPort).savePendingWithOutbox(captor.capture(), any());
        assertThat(captor.getValue().parentSummaryId()).isEqualTo(BASE_SUMMARY_ID);
        assertThat(captor.getValue().revisionNo()).isEqualTo(4);
        assertThat(result.summaryStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("확정된 요약은 개선 기준으로 사용할 수 없다")
    void rejectsConfirmedBase() {
        when(commandPort.findImprovementBaseForUpdate(
                COMPANY_ID, NOTICE_ID, USER_ID, BASE_SUMMARY_ID
        )).thenReturn(Optional.of(base(3, BidNoticeSummaryStatus.COMPLETED, true)));

        assertNotEditable(command(BASE_SUMMARY_ID));
    }

    @Test
    @DisplayName("20차 요약에서는 다음 개선 요청을 생성할 수 없다")
    void rejectsRevisionLimit() {
        when(commandPort.findImprovementBaseForUpdate(
                COMPANY_ID, NOTICE_ID, USER_ID, BASE_SUMMARY_ID
        )).thenReturn(Optional.of(base(20, BidNoticeSummaryStatus.COMPLETED, false)));

        assertNotEditable(command(BASE_SUMMARY_ID));
    }

    @Test
    @DisplayName("완료되지 않은 요약은 개선 기준으로 사용할 수 없다")
    void rejectsIncompleteBase() {
        when(commandPort.findImprovementBaseForUpdate(
                COMPANY_ID, NOTICE_ID, USER_ID, BASE_SUMMARY_ID
        )).thenReturn(Optional.of(base(3, BidNoticeSummaryStatus.FAILED, false)));

        assertNotEditable(command(BASE_SUMMARY_ID));
    }

    @Test
    @DisplayName("다른 회사·공고·요청자의 기준 요약은 찾을 수 없는 것으로 처리한다")
    void rejectsInaccessibleBase() {
        when(commandPort.findImprovementBaseForUpdate(
                COMPANY_ID, NOTICE_ID, USER_ID, BASE_SUMMARY_ID
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(command(BASE_SUMMARY_ID)))
                .isInstanceOf(NotFoundException.class)
                .satisfies(exception -> assertThat(((NotFoundException) exception).getErrorCode())
                        .isEqualTo(BiddingErrorCode.BIDDING_SUMMARY_NOT_FOUND));
        verify(commandPort, never()).savePendingWithOutbox(any(), any());
    }

    @Test
    @DisplayName("동시 요청이 활성 처리 유니크 제약과 충돌하면 처리 중 오류로 변환한다")
    void translatesConcurrentActiveProcessingConflict() {
        doThrow(new DataIntegrityViolationException(
                "Duplicate entry for key 'uk_bid_notice_summary_active_processing'"
        )).when(commandPort).savePendingWithOutbox(any(), any());

        assertThatThrownBy(() -> service.create(command(null)))
                .isInstanceOf(ConflictException.class)
                .satisfies(exception -> assertThat(
                        ((ConflictException) exception).getErrorCode()
                ).isEqualTo(BiddingErrorCode.BIDDING_SUMMARY_ALREADY_PROCESSING));
    }

    @Test
    @DisplayName("다른 무결성 제약 위반은 처리 중 충돌로 오인하지 않는다")
    void propagatesUnrelatedIntegrityViolation() {
        DataIntegrityViolationException failure =
                new DataIntegrityViolationException("another_constraint");
        doThrow(failure).when(commandPort)
                .savePendingWithOutbox(any(), any());

        assertThatThrownBy(() -> service.create(command(null)))
                .isSameAs(failure);
    }

    private void assertNotEditable(CreateBidNoticeSummaryCommand command) {
        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(ConflictException.class)
                .satisfies(exception -> assertThat(((ConflictException) exception).getErrorCode())
                        .isEqualTo(BiddingErrorCode.BIDDING_SUMMARY_NOT_EDITABLE));
        verify(commandPort, never()).savePendingWithOutbox(any(), any());
    }

    private CreateBidNoticeSummaryCommand command(Long baseSummaryId) {
        return new CreateBidNoticeSummaryCommand(
                NOTICE_ID,
                USER_ID,
                ROLE,
                "기존 요약의 위험 요소를 더 구체화해줘.",
                baseSummaryId
        );
    }

    private BidNoticeSummaryCommandPort.ImprovementBase base(
            int revisionNo,
            BidNoticeSummaryStatus status,
            boolean confirmed
    ) {
        return new BidNoticeSummaryCommandPort.ImprovementBase(
                BASE_SUMMARY_ID,
                revisionNo,
                status,
                confirmed
        );
    }

    private BidNoticeSummary withId(BidNoticeSummary summary) {
        return new BidNoticeSummary(
                100L,
                summary.companyId(),
                summary.noticeId(),
                summary.parentSummaryId(),
                summary.revisionNo(),
                summary.requestedBy(),
                summary.prompt(),
                summary.summaryStatus(),
                summary.processingAttemptId(),
                summary.retryCount(),
                NOW
        );
    }
}
