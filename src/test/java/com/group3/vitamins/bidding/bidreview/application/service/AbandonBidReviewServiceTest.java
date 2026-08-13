package com.group3.vitamins.bidding.bidreview.application.service;

import com.group3.vitamins.bidding.bidreview.application.command.AbandonBidReviewCommand;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewCommandPort;
import com.group3.vitamins.bidding.bidreview.domain.exception.BidReviewErrorCode;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReview;
import com.group3.vitamins.bidding.bidreview.domain.repository.BidReviewRepository;
import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
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

@DisplayName("AbandonBidReviewService 입찰 문서 검토 포기")
class AbandonBidReviewServiceTest {

    private static final Long REVIEW_ID = 71L;
    private static final Long COMPANY_ID = 10L;
    private static final Long NOTICE_ID = 1L;
    private static final String USER_ID = "EMP001";
    private static final String ROLE = "MEMBER";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 13, 10, 0);

    private BidReviewRepository bidReviewRepository;
    private BidReviewCommandPort bidReviewCommandPort;
    private BiddingAccessPolicy biddingAccessPolicy;
    private AbandonBidReviewService service;

    @BeforeEach
    void setUp() {
        bidReviewRepository = mock(BidReviewRepository.class);
        bidReviewCommandPort = mock(BidReviewCommandPort.class);
        biddingAccessPolicy = mock(BiddingAccessPolicy.class);
        CurrentCompanyIdProvider companyIdProvider = mock(CurrentCompanyIdProvider.class);
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-13T01:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );

        service = new AbandonBidReviewService(
                bidReviewRepository, bidReviewCommandPort, biddingAccessPolicy, companyIdProvider, clock
        );

        when(companyIdProvider.currentCompanyId()).thenReturn(COMPANY_ID);
    }

    @Test
    @DisplayName("본인이 요청한 검토를 포기하고 잠금 조회 시점 기준으로 저장된 결과를 반환한다")
    void abandonsOwnReview() {
        when(bidReviewRepository.findById(REVIEW_ID))
                .thenReturn(Optional.of(ownReview()));
        BidReview abandoned = withId(ownReview().abandon(NOW), REVIEW_ID);
        when(bidReviewCommandPort.saveAbandonedWithCleanupOutbox(REVIEW_ID, NOW))
                .thenReturn(abandoned);

        var result = service.abandon(new AbandonBidReviewCommand(REVIEW_ID, USER_ID, ROLE));

        assertThat(result.reviewId()).isEqualTo(REVIEW_ID);
        assertThat(result.reviewStatus()).isEqualTo("ABANDONED");
        verify(bidReviewCommandPort).saveAbandonedWithCleanupOutbox(REVIEW_ID, NOW);
    }

    @Test
    @DisplayName("존재하지 않는 검토면 404를 던진다")
    void rejectsWhenNotFound() {
        when(bidReviewRepository.findById(REVIEW_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.abandon(
                new AbandonBidReviewCommand(REVIEW_ID, USER_ID, ROLE)
        )).isInstanceOf(NotFoundException.class)
                .satisfies(exception -> assertThat(((NotFoundException) exception).getErrorCode())
                        .isEqualTo(BidReviewErrorCode.BIDDING_REVIEW_NOT_FOUND));

        verifyNoInteractions(bidReviewCommandPort);
    }

    @Test
    @DisplayName("다른 회사의 검토면 403으로 거절한다")
    void rejectsWhenDifferentCompany() {
        when(bidReviewRepository.findById(REVIEW_ID))
                .thenReturn(Optional.of(reviewOwnedBy(999L, USER_ID)));

        assertThatThrownBy(() -> service.abandon(
                new AbandonBidReviewCommand(REVIEW_ID, USER_ID, ROLE)
        )).isInstanceOf(ForbiddenException.class)
                .satisfies(exception -> assertThat(((ForbiddenException) exception).getErrorCode())
                        .isEqualTo(BidReviewErrorCode.BIDDING_REVIEW_ACCESS_DENIED));

        verifyNoInteractions(bidReviewCommandPort);
    }

    @Test
    @DisplayName("같은 회사라도 다른 요청자의 검토면 403으로 거절한다")
    void rejectsWhenDifferentRequester() {
        when(bidReviewRepository.findById(REVIEW_ID))
                .thenReturn(Optional.of(reviewOwnedBy(COMPANY_ID, "EMP999")));

        assertThatThrownBy(() -> service.abandon(
                new AbandonBidReviewCommand(REVIEW_ID, USER_ID, ROLE)
        )).isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(bidReviewCommandPort);
    }

    @Test
    @DisplayName("잠금 조회 시점에 포기 불가능한 상태면 409로 변환한다")
    void convertsIllegalStateToConflict() {
        when(bidReviewRepository.findById(REVIEW_ID))
                .thenReturn(Optional.of(ownReview()));
        when(bidReviewCommandPort.saveAbandonedWithCleanupOutbox(REVIEW_ID, NOW))
                .thenThrow(new IllegalStateException("이미 정리됨"));

        assertThatThrownBy(() -> service.abandon(
                new AbandonBidReviewCommand(REVIEW_ID, USER_ID, ROLE)
        )).isInstanceOf(ConflictException.class)
                .satisfies(exception -> assertThat(((ConflictException) exception).getErrorCode())
                        .isEqualTo(BidReviewErrorCode.BIDDING_REVIEW_NOT_ABANDONABLE));
    }

    @Test
    @DisplayName("입찰 관리 권한이 없으면 조회 전에 403으로 막힌다")
    void shortCircuitsOnAccessDenied() {
        doThrow(new ForbiddenException(BiddingErrorCode.BIDDING_ACCESS_PERMISSION_REQUIRED))
                .when(biddingAccessPolicy).assertAccess(USER_ID, ROLE);

        assertThatThrownBy(() -> service.abandon(
                new AbandonBidReviewCommand(REVIEW_ID, USER_ID, ROLE)
        )).isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(bidReviewRepository, bidReviewCommandPort);
    }

    private BidReview ownReview() {
        return reviewOwnedBy(COMPANY_ID, USER_ID);
    }

    private BidReview reviewOwnedBy(Long companyId, String requestedBy) {
        return BidReview.createPending(
                companyId, NOTICE_ID, requestedBy, "검토 지시", "attempt-1", NOW
        );
    }

    // createPending()은 아직 DB에 없는 신규 검토를 위한 것이라 reviewId가 null이다 — 이미 저장된
    // 행을 흉내내는 mock 반환값에는 실제 ID가 있어야 하므로 레코드를 직접 복제해 채운다.
    private BidReview withId(BidReview review, Long reviewId) {
        return new BidReview(
                reviewId,
                review.companyId(),
                review.noticeId(),
                review.requestedBy(),
                review.projectId(),
                review.prompt(),
                review.reviewStatus(),
                review.processingAttemptId(),
                review.retryCount(),
                review.result(),
                review.errorCode(),
                review.errorMessage(),
                review.completedAt(),
                review.expiresAt(),
                review.abandonedAt(),
                review.cleanupStartedAt(),
                review.cleanupCompletedAt(),
                review.createdAt(),
                review.updatedAt()
        );
    }
}
