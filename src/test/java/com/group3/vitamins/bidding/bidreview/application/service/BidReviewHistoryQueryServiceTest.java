package com.group3.vitamins.bidding.bidreview.application.service;

import com.group3.vitamins.bidding.bidreview.application.port.BidReviewHistoryQueryPort;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewHistoryQueryPort.HistoryRow;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewNoticeDocumentPort;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewNoticeDocumentPort.NoticeSnapshot;
import com.group3.vitamins.bidding.bidreview.application.query.GetBidReviewHistoryQuery;
import com.group3.vitamins.bidding.bidreview.application.result.BidReviewHistoryResult;
import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("BidReviewHistoryQueryService 공고별 입찰 문서 검토 이력 조회")
class BidReviewHistoryQueryServiceTest {

    private static final Long COMPANY_ID = 10L;
    private static final Long NOTICE_ID = 1L;
    private static final String USER_ID = "EMP001";
    private static final String ROLE = "MEMBER";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 12, 10, 0);

    private BidReviewNoticeDocumentPort noticeDocumentPort;
    private BidReviewHistoryQueryPort historyQueryPort;
    private BiddingAccessPolicy biddingAccessPolicy;
    private BidReviewHistoryQueryService service;

    @BeforeEach
    void setUp() {
        noticeDocumentPort = mock(BidReviewNoticeDocumentPort.class);
        historyQueryPort = mock(BidReviewHistoryQueryPort.class);
        biddingAccessPolicy = mock(BiddingAccessPolicy.class);
        CurrentCompanyIdProvider companyIdProvider = mock(CurrentCompanyIdProvider.class);

        service = new BidReviewHistoryQueryService(
                noticeDocumentPort, historyQueryPort, biddingAccessPolicy, companyIdProvider
        );

        when(companyIdProvider.currentCompanyId()).thenReturn(COMPANY_ID);
        when(noticeDocumentPort.findAccessibleNotice(COMPANY_ID, NOTICE_ID))
                .thenReturn(Optional.of(new NoticeSnapshot(NOTICE_ID, "스마트시티 통합관제 용역", null)));
    }

    @Test
    @DisplayName("본인이 요청한 이력을 최신순으로 반환한다")
    void returnsHistory() {
        when(historyQueryPort.findHistory(COMPANY_ID, NOTICE_ID, USER_ID))
                .thenReturn(List.of(new HistoryRow(
                        71L, "COMPLETED", "검토 지시", NOW, NOW, null, null
                )));

        BidReviewHistoryResult result = service.get(
                new GetBidReviewHistoryQuery(NOTICE_ID, USER_ID, ROLE)
        );

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).reviewId()).isEqualTo(71L);
    }

    @Test
    @DisplayName("현재 회사에서 접근 불가한 공고면 404를 던진다")
    void rejectsWhenNoticeNotAccessible() {
        when(noticeDocumentPort.findAccessibleNotice(COMPANY_ID, NOTICE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(
                new GetBidReviewHistoryQuery(NOTICE_ID, USER_ID, ROLE)
        ))
                .isInstanceOf(NotFoundException.class)
                .satisfies(exception -> assertThat(((NotFoundException) exception).getErrorCode())
                        .isEqualTo(BiddingErrorCode.BIDDING_NOTICE_NOT_FOUND));

        verifyNoInteractions(historyQueryPort);
    }

    @Test
    @DisplayName("입찰 관리 권한이 없으면 조회 전에 403으로 막힌다")
    void shortCircuitsOnAccessDenied() {
        doThrow(new ForbiddenException(BiddingErrorCode.BIDDING_ACCESS_PERMISSION_REQUIRED))
                .when(biddingAccessPolicy).assertAccess(USER_ID, ROLE);

        assertThatThrownBy(() -> service.get(
                new GetBidReviewHistoryQuery(NOTICE_ID, USER_ID, ROLE)
        )).isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(noticeDocumentPort, historyQueryPort);
    }
}
