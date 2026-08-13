package com.group3.vitamins.bidding.bidreview.application.service;

import com.group3.vitamins.bidding.bidreview.application.port.BidReviewDetailQueryPort;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewDetailQueryPort.CitationRow;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewDetailQueryPort.DocumentRow;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewDetailQueryPort.ReviewRow;
import com.group3.vitamins.bidding.bidreview.application.query.GetBidReviewDetailQuery;
import com.group3.vitamins.bidding.bidreview.application.result.BidReviewDetailResult;
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

@DisplayName("BidReviewDetailQueryService 입찰 문서 검토 상세 조회")
class BidReviewDetailQueryServiceTest {

    private static final Long COMPANY_ID = 10L;
    private static final Long REVIEW_ID = 71L;
    private static final Long NOTICE_ID = 1L;
    private static final String USER_ID = "EMP001";
    private static final String ROLE = "MEMBER";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 12, 10, 0);

    private BidReviewDetailQueryPort detailQueryPort;
    private BiddingAccessPolicy biddingAccessPolicy;
    private BidReviewDetailQueryService service;

    @BeforeEach
    void setUp() {
        detailQueryPort = mock(BidReviewDetailQueryPort.class);
        biddingAccessPolicy = mock(BiddingAccessPolicy.class);
        CurrentCompanyIdProvider companyIdProvider = mock(CurrentCompanyIdProvider.class);

        service = new BidReviewDetailQueryService(
                detailQueryPort, biddingAccessPolicy, companyIdProvider
        );

        when(companyIdProvider.currentCompanyId()).thenReturn(COMPANY_ID);
    }

    @Test
    @DisplayName("본인 회사의 검토면 문서·근거 목록까지 조회한다")
    void returnsDetailForOwnReview() {
        when(detailQueryPort.findReview(REVIEW_ID))
                .thenReturn(Optional.of(ownReview()));
        when(detailQueryPort.findDocuments(REVIEW_ID))
                .thenReturn(List.of(new DocumentRow(
                        "BID_ATTACHMENT", 31L, null, null, "제안요청서.pdf", "READY"
                )));
        when(detailQueryPort.findCitations(REVIEW_ID))
                .thenReturn(List.of(new CitationRow(
                        1, "INTERNAL_REFERENCE", null, 501L, null, "원가계산_기준.pdf", 3, null, "발췌문"
                )));

        BidReviewDetailResult result = service.get(
                new GetBidReviewDetailQuery(REVIEW_ID, USER_ID, ROLE)
        );

        assertThat(result.reviewId()).isEqualTo(REVIEW_ID);
        assertThat(result.documents()).hasSize(1);
        assertThat(result.citations()).hasSize(1);
    }

    @Test
    @DisplayName("존재하지 않는 검토면 404를 던진다")
    void rejectsWhenNotFound() {
        when(detailQueryPort.findReview(REVIEW_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(
                new GetBidReviewDetailQuery(REVIEW_ID, USER_ID, ROLE)
        )).isInstanceOf(NotFoundException.class);

        verify(detailQueryPort, never()).findDocuments(any());
        verify(detailQueryPort, never()).findCitations(any());
    }

    @Test
    @DisplayName("다른 회사의 검토면 403으로 거절한다")
    void rejectsWhenDifferentCompany() {
        when(detailQueryPort.findReview(REVIEW_ID))
                .thenReturn(Optional.of(reviewOwnedBy(999L, USER_ID)));

        assertThatThrownBy(() -> service.get(
                new GetBidReviewDetailQuery(REVIEW_ID, USER_ID, ROLE)
        )).isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("같은 회사라도 다른 요청자의 검토면 403으로 거절한다")
    void rejectsWhenDifferentRequester() {
        when(detailQueryPort.findReview(REVIEW_ID))
                .thenReturn(Optional.of(reviewOwnedBy(COMPANY_ID, "EMP999")));

        assertThatThrownBy(() -> service.get(
                new GetBidReviewDetailQuery(REVIEW_ID, USER_ID, ROLE)
        )).isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("입찰 관리 권한이 없으면 조회 전에 403으로 막힌다")
    void shortCircuitsOnAccessDenied() {
        doThrow(new ForbiddenException(BiddingErrorCode.BIDDING_ACCESS_PERMISSION_REQUIRED))
                .when(biddingAccessPolicy).assertAccess(USER_ID, ROLE);

        assertThatThrownBy(() -> service.get(
                new GetBidReviewDetailQuery(REVIEW_ID, USER_ID, ROLE)
        )).isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(detailQueryPort);
    }

    private ReviewRow ownReview() {
        return reviewOwnedBy(COMPANY_ID, USER_ID);
    }

    private ReviewRow reviewOwnedBy(Long companyId, String requestedBy) {
        return new ReviewRow(
                REVIEW_ID, companyId, NOTICE_ID, requestedBy,
                "검토 지시", "COMPLETED", "검토 결과", null,
                NOW, NOW, null, null
        );
    }
}
