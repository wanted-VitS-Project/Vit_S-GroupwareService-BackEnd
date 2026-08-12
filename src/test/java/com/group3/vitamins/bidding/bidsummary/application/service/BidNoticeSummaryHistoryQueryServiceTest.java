package com.group3.vitamins.bidding.bidsummary.application.service;

import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryHistoryQueryPort;
import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryNoticePort;
import com.group3.vitamins.bidding.bidsummary.application.query.GetBidNoticeSummaryHistoryQuery;
import com.group3.vitamins.bidding.bidsummary.application.result.BidNoticeSummaryHistoryItemResult;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("BidNoticeSummaryHistoryQueryService 요약 이력 조회")
class BidNoticeSummaryHistoryQueryServiceTest {

    private static final Long COMPANY_ID = 10L;
    private static final Long NOTICE_ID = 317L;
    private static final String USER_ID = "vitas-USER001";

    private BidNoticeSummaryHistoryQueryPort historyPort;
    private BidNoticeSummaryNoticePort noticePort;
    private BiddingPageAccessPort accessPort;
    private CurrentCompanyIdProvider companyIdProvider;
    private BidNoticeSummaryHistoryQueryService service;

    @BeforeEach
    void setUp() {
        historyPort = mock(BidNoticeSummaryHistoryQueryPort.class);
        noticePort = mock(BidNoticeSummaryNoticePort.class);
        accessPort = mock(BiddingPageAccessPort.class);
        companyIdProvider = mock(CurrentCompanyIdProvider.class);
        service = new BidNoticeSummaryHistoryQueryService(
                historyPort, noticePort, new BiddingAccessPolicy(accessPort),
                companyIdProvider
        );
        when(accessPort.hasAccess(USER_ID, "ADMIN")).thenReturn(true);
        when(companyIdProvider.currentCompanyId()).thenReturn(COMPANY_ID);
        when(noticePort.findAccessibleNotice(COMPANY_ID, NOTICE_ID))
                .thenReturn(Optional.of(mock(BidNoticeSummaryNoticePort.BidNoticeSnapshot.class)));
    }

    @Test
    @DisplayName("내 최신 요약 ID와 조회 가능한 이력을 페이지 정보와 함께 반환한다")
    void returnsHistoryPage() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 12, 10, 0);
        var item = new BidNoticeSummaryHistoryItemResult(
                32L, 31L, 2, "COMPLETED", "위험을 보강해줘",
                false, true, null, now, null
        );
        when(historyPort.findHistory(COMPANY_ID, NOTICE_ID, USER_ID, 20, 20))
                .thenReturn(List.of(item));
        when(historyPort.countHistory(COMPANY_ID, NOTICE_ID, USER_ID))
                .thenReturn(21L);
        when(historyPort.findLatestMineSummaryId(COMPANY_ID, NOTICE_ID, USER_ID))
                .thenReturn(32L);

        var result = service.get(query(1, 20));

        assertThat(result.latestMySummaryId()).isEqualTo(32L);
        assertThat(result.content()).containsExactly(item);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.page()).isEqualTo(1);
    }

    @Test
    @DisplayName("현재 회사가 접근할 수 없는 공고면 이력 조회를 시작하지 않는다")
    void rejectsInaccessibleNotice() {
        when(noticePort.findAccessibleNotice(COMPANY_ID, NOTICE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(query(0, 20)))
                .isInstanceOf(NotFoundException.class)
                .satisfies(exception -> assertThat(((NotFoundException) exception).getErrorCode())
                        .isEqualTo(BiddingErrorCode.BIDDING_NOTICE_NOT_FOUND));

        verifyNoInteractions(historyPort);
    }

    @Test
    @DisplayName("페이지 크기가 50을 넘으면 권한과 저장소 조회 전에 거부한다")
    void rejectsOversizedPage() {
        assertThatThrownBy(() -> service.get(query(0, 51)))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(accessPort, companyIdProvider, noticePort, historyPort);
    }

    private GetBidNoticeSummaryHistoryQuery query(int page, int size) {
        return new GetBidNoticeSummaryHistoryQuery(
                NOTICE_ID, page, size, USER_ID, "ADMIN"
        );
    }
}
