package com.group3.vitamins.bidding.bidsummary.application.service;

import com.group3.vitamins.bidding.bidsummary.application.command.ConfirmBidNoticeSummaryCommand;
import com.group3.vitamins.bidding.bidsummary.application.command.SummaryPatchField;
import com.group3.vitamins.bidding.bidsummary.application.command.UpdateBidNoticeSummaryCommand;
import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryManagementPort;
import com.group3.vitamins.bidding.bidsummary.domain.model.BidNoticeSummaryDetails;
import com.group3.vitamins.bidding.bidsummary.domain.model.BidNoticeSummaryStatus;
import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.bidding.collectioncondition.application.port.BiddingPageAccessPort;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
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
import static org.mockito.Mockito.*;

@DisplayName("BidNoticeSummaryReviewService 수정 및 확정")
class BidNoticeSummaryReviewServiceTest {

    private static final Long COMPANY_ID = 10L;
    private static final Long SUMMARY_ID = 31L;
    private static final String USER_ID = "vitas-USER001";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 12, 9, 0);

    private BidNoticeSummaryManagementPort managementPort;
    private BiddingPageAccessPort accessPort;
    private CurrentCompanyIdProvider companyIdProvider;
    private BidNoticeSummaryReviewService service;

    @BeforeEach
    void setUp() {
        managementPort = mock(BidNoticeSummaryManagementPort.class);
        accessPort = mock(BiddingPageAccessPort.class);
        companyIdProvider = mock(CurrentCompanyIdProvider.class);
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-12T00:00:00Z"), ZoneId.of("Asia/Seoul")
        );
        service = new BidNoticeSummaryReviewService(
                managementPort, new BiddingAccessPolicy(accessPort),
                companyIdProvider, clock
        );
        when(accessPort.hasAccess(USER_ID, "ADMIN")).thenReturn(true);
        when(companyIdProvider.currentCompanyId()).thenReturn(COMPANY_ID);
    }

    @Test
    @DisplayName("전달한 요약만 바꾸고 생략한 요약은 유지한다")
    void updatesOnlyPresentFields() {
        BidNoticeSummaryDetails current = details(BidNoticeSummaryStatus.COMPLETED, false);
        when(managementPort.findOwnedForUpdate(COMPANY_ID, SUMMARY_ID, USER_ID))
                .thenReturn(Optional.of(current));
        when(managementPort.updateSummaries(eq(SUMMARY_ID), any(), eq(NOW)))
                .thenAnswer(invocation -> withValues(
                        current, invocation.getArgument(1), false
                ));

        var result = service.update(updateCommand("수정된 위험"));

        ArgumentCaptor<BidNoticeSummaryManagementPort.SummaryValues> captor =
                ArgumentCaptor.forClass(BidNoticeSummaryManagementPort.SummaryValues.class);
        verify(managementPort).updateSummaries(eq(SUMMARY_ID), captor.capture(), eq(NOW));
        assertThat(captor.getValue().overviewSummary()).isEqualTo("개요");
        assertThat(captor.getValue().riskSummary()).isEqualTo("수정된 위험");
        assertThat(result.riskSummary()).isEqualTo("수정된 위험");
    }

    @Test
    @DisplayName("완료 전이거나 이미 확정된 요약은 수정할 수 없다")
    void rejectsNonEditableSummary() {
        when(managementPort.findOwnedForUpdate(COMPANY_ID, SUMMARY_ID, USER_ID))
                .thenReturn(Optional.of(details(BidNoticeSummaryStatus.PROCESSING, false)));

        assertError(() -> service.update(updateCommand("수정")),
                ConflictException.class, BiddingErrorCode.BIDDING_SUMMARY_NOT_EDITABLE);
        verify(managementPort, never()).updateSummaries(anyLong(), any(), any());
    }

    @Test
    @DisplayName("수정 필드가 없거나 공백이면 저장소 조회 전에 거부한다")
    void rejectsInvalidPatch() {
        assertThatThrownBy(() -> service.update(updateCommand(" ")))
                .isInstanceOf(ValidationException.class)
                .satisfies(exception -> assertThat(((ValidationException) exception).getErrorCode())
                        .isEqualTo(BiddingErrorCode.BIDDING_INVALID_SUMMARY_UPDATE));
        verifyNoInteractions(accessPort, companyIdProvider, managementPort);
    }

    @Test
    @DisplayName("본인이 요청한 완료 요약을 확정한다")
    void confirmsCompletedSummary() {
        BidNoticeSummaryDetails current = details(BidNoticeSummaryStatus.COMPLETED, false);
        when(managementPort.findOwnedForUpdate(COMPANY_ID, SUMMARY_ID, USER_ID))
                .thenReturn(Optional.of(current));
        when(managementPort.confirm(SUMMARY_ID, USER_ID, NOW))
                .thenReturn(withValues(current, values(current), true));

        var result = service.confirm(confirmCommand());

        assertThat(result.confirmed()).isTrue();
        assertThat(result.projectCreationAllowed()).isTrue();
        verify(managementPort).confirm(SUMMARY_ID, USER_ID, NOW);
    }

    @Test
    @DisplayName("이미 확정된 요약은 다시 확정할 수 없다")
    void rejectsAlreadyConfirmedSummary() {
        when(managementPort.findOwnedForUpdate(COMPANY_ID, SUMMARY_ID, USER_ID))
                .thenReturn(Optional.of(details(BidNoticeSummaryStatus.COMPLETED, true)));

        assertError(() -> service.confirm(confirmCommand()),
                ConflictException.class, BiddingErrorCode.BIDDING_SUMMARY_ALREADY_CONFIRMED);
    }

    @Test
    @DisplayName("다른 사용자의 요약은 존재 여부를 숨기고 Not Found를 반환한다")
    void rejectsNonOwner() {
        when(managementPort.findOwnedForUpdate(COMPANY_ID, SUMMARY_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertError(() -> service.confirm(confirmCommand()),
                NotFoundException.class, BiddingErrorCode.BIDDING_SUMMARY_NOT_FOUND);
    }

    private UpdateBidNoticeSummaryCommand updateCommand(String riskSummary) {
        return new UpdateBidNoticeSummaryCommand(
                SUMMARY_ID,
                SummaryPatchField.absent(), SummaryPatchField.absent(),
                SummaryPatchField.absent(), SummaryPatchField.absent(),
                SummaryPatchField.absent(), SummaryPatchField.of(riskSummary),
                USER_ID, "ADMIN"
        );
    }

    private ConfirmBidNoticeSummaryCommand confirmCommand() {
        return new ConfirmBidNoticeSummaryCommand(SUMMARY_ID, USER_ID, "ADMIN");
    }

    private BidNoticeSummaryDetails details(
            BidNoticeSummaryStatus status, boolean confirmed
    ) {
        return new BidNoticeSummaryDetails(
                SUMMARY_ID, COMPANY_ID, 317L, USER_ID, "검토해줘", status,
                "개요", "금액", "일정", "자격", "과업", "위험",
                confirmed, confirmed ? USER_ID : null,
                confirmed ? NOW : null, null, null, NOW, NOW, NOW
        );
    }

    private BidNoticeSummaryManagementPort.SummaryValues values(
            BidNoticeSummaryDetails details
    ) {
        return new BidNoticeSummaryManagementPort.SummaryValues(
                details.overviewSummary(), details.amountSummary(),
                details.scheduleSummary(), details.qualificationSummary(),
                details.taskSummary(), details.riskSummary()
        );
    }

    private BidNoticeSummaryDetails withValues(
            BidNoticeSummaryDetails current,
            BidNoticeSummaryManagementPort.SummaryValues values,
            boolean confirmed
    ) {
        return new BidNoticeSummaryDetails(
                current.summaryId(), current.companyId(), current.noticeId(),
                current.requestedBy(), current.prompt(), current.summaryStatus(),
                values.overviewSummary(), values.amountSummary(),
                values.scheduleSummary(), values.qualificationSummary(),
                values.taskSummary(), values.riskSummary(), confirmed,
                confirmed ? USER_ID : null, confirmed ? NOW : null,
                current.projectId(), current.errorMessage(), current.requestedAt(),
                current.completedAt(), NOW
        );
    }

    private void assertError(
            Runnable invocation,
            Class<? extends RuntimeException> type,
            BiddingErrorCode errorCode
    ) {
        assertThatThrownBy(invocation::run)
                .isInstanceOf(type)
                .satisfies(exception -> {
                    if (exception instanceof ConflictException conflict) {
                        assertThat(conflict.getErrorCode()).isEqualTo(errorCode);
                    } else if (exception instanceof NotFoundException notFound) {
                        assertThat(notFound.getErrorCode()).isEqualTo(errorCode);
                    }
                });
    }
}
