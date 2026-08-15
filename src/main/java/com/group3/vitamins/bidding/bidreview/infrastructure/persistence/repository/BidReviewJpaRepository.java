package com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository;

import com.group3.vitamins.bidding.bidreview.domain.model.BidReviewStatus;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
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

    // 만료 후보 — 잠금 없이 가볍게 조회한다. 실제 점유는 findForWorkerUpdate로 한 건씩 한다.
    @Query(value = """
        SELECT bid_review_id
        FROM bid_review
        WHERE review_status IN ('COMPLETED', 'FAILED')
          AND project_id IS NULL
          AND expires_at <= :now
          AND cleanup_started_at IS NULL
        ORDER BY expires_at, bid_review_id
        LIMIT :batchSize
        """, nativeQuery = true)
    List<Long> findExpiredCandidateIds(
            @Param("now") LocalDateTime now,
            @Param("batchSize") int batchSize
    );

    // 정상 흐름에서는 검토와 outbox가 같은 트랜잭션에 저장돼 나오지 않아야 하는 상태다 - 그래도
    // 살아있는(PENDING) outbox가 하나도 없이 오래 멈춘 검토를 방어적으로 찾아 재발행 스케줄러가 훑는다
    // (bidsummary의 findOrphanedCandidateIds와 동일 패턴).
    @Query(value = """
            SELECT r.bid_review_id
            FROM bid_review r
            WHERE r.review_status IN ('PENDING', 'PROCESSING')
              AND r.updated_at < :staleBefore
              AND NOT EXISTS (
                  SELECT 1 FROM bid_review_outbox o
                  WHERE o.bid_review_id = r.bid_review_id
                    AND o.publish_status = 'PENDING'
              )
            ORDER BY r.bid_review_id
            LIMIT :batchLimit
            """, nativeQuery = true)
    List<Long> findOrphanedCandidateIds(
            @Param("staleBefore") LocalDateTime staleBefore,
            @Param("batchLimit") int batchLimit
    );
}