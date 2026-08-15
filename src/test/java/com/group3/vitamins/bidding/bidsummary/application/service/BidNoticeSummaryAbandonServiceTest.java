package com.group3.vitamins.bidding.bidsummary.application.service;

import com.group3.vitamins.bidding.bidsummary.application.command.AbandonBidNoticeSummaryCommand;
import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryManagementPort;
import com.group3.vitamins.bidding.bidsummary.domain.model.BidNoticeSummaryDetails;
import com.group3.vitamins.bidding.bidsummary.domain.model.BidNoticeSummaryStatus;
import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.bidding.collectioncondition.application.port.BiddingPageAccessPort;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
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
import static org.mockito.Mockito.*;

@DisplayName("BidNoticeSummaryAbandonService 요약 중단")
class BidNoticeSummaryAbandonServiceTest {

    private static final Long COMPANY_ID = 10L;
    private static final Long SUMMARY_ID = 31L;
    private static final String USER_ID = "vitas-USER001";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 12, 9, 0);

    private BidNoticeSummaryManagementPort managementPort;
    private BiddingPageAccessPort accessPort;
    private CurrentCompanyIdProvider companyIdProvider;
    private BidNoticeSummaryAbandonService service;

    @BeforeEach
    void setUp() {
        managementPort = mock(BidNoticeSummaryManagementPort.class);
        accessPort = mock(BiddingPageAccessPort.class);
        companyIdProvider = mock(CurrentCompanyIdProvider.class);
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-12T00:00:00Z"), ZoneId.of("Asia/Seoul")
        );
        service = new BidNoticeSummaryAbandonService(
                managementPort, new BiddingAccessPolicy(accessPort),
                companyIdProvider, clock
        );
        when(accessPort.hasAccess(USER_ID, "ADMIN")).thenReturn(true);
        when(companyIdProvider.currentCompanyId()).thenReturn(COMPANY_ID);
    }

    @Test
    @DisplayName("진행 중인 요약을 중단하고 ABANDONED 상태를 반환한다")
    void abandonsInProgressSummary() {
        when(managementPort.findOwnedForUpdate(COMPANY_ID, SUMMARY_ID, USER_ID))
                .thenReturn(Optional.of(details(BidNoticeSummaryStatus.PENDING)));
        when(managementPort.abandon(SUMMARY_ID, NOW))
                .thenReturn(details(BidNoticeSummaryStatus.ABANDONED));

        var result = service.abandon(command());

        assertThat(result.summaryId()).isEqualTo(SUMMARY_ID);
        assertThat(result.summaryStatus()).isEqualTo("ABANDONED");
        assertThat(result.abandonedAt()).isEqualTo(NOW);
        verify(managementPort).abandon(SUMMARY_ID, NOW);
    }

    @Test
    @DisplayName("현재 회사·요청자 소유가 아니면 404를 던진다")
    void rejectsWhenNotOwned() {
        when(managementPort.findOwnedForUpdate(COMPANY_ID, SUMMARY_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.abandon(command()))
                .isInstanceOf(NotFoundException.class)
                .satisfies(exception -> assertThat(((DomainException) exception).getErrorCode())
                        .isEqualTo(BiddingErrorCode.BIDDING_SUMMARY_NOT_FOUND));

        verify(managementPort, never()).abandon(any(), any());
    }

    @Test
    @DisplayName("진행 중이 아닌 요약을 중단하려 하면 409를 던진다")
    void rejectsAbandoningNonInProgressSummary() {
        when(managementPort.findOwnedForUpdate(COMPANY_ID, SUMMARY_ID, USER_ID))
                .thenReturn(Optional.of(details(BidNoticeSummaryStatus.COMPLETED)));
        when(managementPort.abandon(SUMMARY_ID, NOW))
                .thenThrow(new IllegalStateException("진행 중이 아닌 요약은 중단할 수 없습니다."));

        assertThatThrownBy(() -> service.abandon(command()))
                .isInstanceOf(ConflictException.class)
                .satisfies(exception -> assertThat(((DomainException) exception).getErrorCode())
                        .isEqualTo(BiddingErrorCode.BIDDING_SUMMARY_NOT_ABANDONABLE));
    }

    @Test
    @DisplayName("유효하지 않은 요약 ID는 권한과 저장소 조회 전에 거부한다")
    void rejectsInvalidCommand() {
        assertThatThrownBy(() -> service.abandon(
                new AbandonBidNoticeSummaryCommand(0L, USER_ID, "ADMIN")
        )).isInstanceOf(ValidationException.class);

        verifyNoInteractions(managementPort, companyIdProvider);
    }

    @Test
    @DisplayName("입찰 관리 권한이 없으면 조회 전에 403으로 막힌다")
    void rejectsWhenAccessDenied() {
        when(accessPort.hasAccess(USER_ID, "USER")).thenReturn(false);

        assertThatThrownBy(() -> service.abandon(
                new AbandonBidNoticeSummaryCommand(SUMMARY_ID, USER_ID, "USER")
        )).isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(managementPort);
    }

    private AbandonBidNoticeSummaryCommand command() {
        return new AbandonBidNoticeSummaryCommand(SUMMARY_ID, USER_ID, "ADMIN");
    }

    private BidNoticeSummaryDetails details(BidNoticeSummaryStatus status) {
        return new BidNoticeSummaryDetails(
                SUMMARY_ID, COMPANY_ID, 317L, null, 1, USER_ID, "검토해줘", status,
                "개요", "금액", "일정", "자격", "과업", "위험",
                false, null, null, null, null, 0, NOW, NOW, NOW
        );
    }
}
