package com.group3.vitamins.bidding.bidsummary.application.service;

import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryManagementPort;
import com.group3.vitamins.bidding.bidsummary.application.query.GetBidNoticeSummaryQuery;
import com.group3.vitamins.bidding.bidsummary.domain.model.BidNoticeSummaryDetails;
import com.group3.vitamins.bidding.bidsummary.domain.model.BidNoticeSummaryStatus;
import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.bidding.collectioncondition.application.port.BiddingPageAccessPort;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("BidNoticeSummaryQueryService 공개 조회")
class BidNoticeSummaryQueryServiceTest {

    private static final Long COMPANY_ID = 10L;
    private static final Long SUMMARY_ID = 31L;
    private static final String USER_ID = "vitas-USER001";

    private BidNoticeSummaryManagementPort managementPort;
    private BiddingPageAccessPort accessPort;
    private CurrentCompanyIdProvider companyIdProvider;
    private BidNoticeSummaryQueryService service;

    @BeforeEach
    void setUp() {
        managementPort = mock(BidNoticeSummaryManagementPort.class);
        accessPort = mock(BiddingPageAccessPort.class);
        companyIdProvider = mock(CurrentCompanyIdProvider.class);
        service = new BidNoticeSummaryQueryService(
                managementPort,
                new BiddingAccessPolicy(accessPort),
                companyIdProvider
        );
        when(accessPort.hasAccess(USER_ID, "ADMIN")).thenReturn(true);
        when(companyIdProvider.currentCompanyId()).thenReturn(COMPANY_ID);
    }

    @Test
    @DisplayName("본인 요약 또는 같은 회사 확정 요약을 Port 조회 결과로 반환한다")
    void returnsAccessibleSummary() {
        when(managementPort.findAccessible(COMPANY_ID, SUMMARY_ID, USER_ID))
                .thenReturn(Optional.of(details(false)));

        var result = service.get(query());

        assertThat(result.summaryId()).isEqualTo(SUMMARY_ID);
        assertThat(result.summaryStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("접근 가능한 요약이 없으면 존재 여부를 숨기고 Not Found를 반환한다")
    void rejectsInaccessibleSummary() {
        when(managementPort.findAccessible(COMPANY_ID, SUMMARY_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(query()))
                .isInstanceOf(NotFoundException.class)
                .satisfies(exception -> assertThat(((NotFoundException) exception).getErrorCode())
                        .isEqualTo(BiddingErrorCode.BIDDING_SUMMARY_NOT_FOUND));
    }

    @Test
    @DisplayName("유효하지 않은 요약 ID는 권한과 저장소 조회 전에 거부한다")
    void rejectsInvalidId() {
        assertThatThrownBy(() -> service.get(
                new GetBidNoticeSummaryQuery(0L, USER_ID, "ADMIN")
        )).isInstanceOf(ValidationException.class);

        verifyNoInteractions(accessPort, companyIdProvider, managementPort);
    }

    private GetBidNoticeSummaryQuery query() {
        return new GetBidNoticeSummaryQuery(SUMMARY_ID, USER_ID, "ADMIN");
    }

    private BidNoticeSummaryDetails details(boolean confirmed) {
        LocalDateTime now = LocalDateTime.of(2026, 8, 12, 9, 0);
        return new BidNoticeSummaryDetails(
                SUMMARY_ID, COMPANY_ID, 317L, USER_ID, "검토해줘",
                BidNoticeSummaryStatus.COMPLETED,
                "개요", "금액", "일정", "자격", "과업", "위험",
                confirmed, confirmed ? USER_ID : null,
                confirmed ? now : null, null, null, now, now, now
        );
    }
}
