package com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository;

import com.group3.vitamins.bidding.bidreview.domain.model.BidReviewStatus;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface BidReviewJpaRepository
        extends JpaRepository<BidReviewJpaEntity, Long> {

    Optional<BidReviewJpaEntity>
    findByReviewIdAndProcessingAttemptId(
            Long reviewId,
            String processingAttemptId
    );

    boolean existsByCompanyIdAndNoticeIdAndRequestedByAndReviewStatusIn(
            Long companyId,
            Long noticeId,
            String requestedBy,
            Collection<BidReviewStatus> reviewStatuses
    );

    // 무엇: 검토 행을 쓰기 잠금으로 조회합니다.
    // 왜: Worker의 작업 조회·진행상황 갱신·완료·실패 콜백이 동시에 같은 검토를 처리하지 못하게 막습니다
    // (bidsummary의 findForWorkerUpdate와 동일 패턴).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select review from BidReviewJpaEntity review where review.reviewId = :reviewId")
    Optional<BidReviewJpaEntity> findForWorkerUpdate(
            @Param("reviewId") Long reviewId
    );
}