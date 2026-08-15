package com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.repository;

import com.group3.vitamins.bidding.bidsummary.domain.model.BidNoticeSummaryStatus;
import com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.entity.BidNoticeSummaryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface BidNoticeSummaryJpaRepository
        extends JpaRepository<BidNoticeSummaryJpaEntity, Long> {

    boolean existsByCompanyIdAndNoticeIdAndRequestedByAndSummaryStatusInAndDeletedAtIsNull(
            Long companyId,
            Long noticeId,
            String requestedBy,
            Collection<BidNoticeSummaryStatus> summaryStatuses
    );
    //무엇: 요약 행을 쓰기 잠금으로 조회합니다.
    //왜: 두 worker의 작업 조회나 callback이 동시에 처리되는 것을 방지합니다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select summary
        from BidNoticeSummaryJpaEntity summary
        where summary.summaryId = :summaryId
          and summary.deletedAt is null
        """)
    Optional<BidNoticeSummaryJpaEntity> findForWorkerUpdate(
            @Param("summaryId") Long summaryId
    );

    @Query("""
        select summary
        from BidNoticeSummaryJpaEntity summary
        where summary.summaryId = :summaryId
          and summary.companyId = :companyId
          and summary.deletedAt is null
          and (summary.requestedBy = :userId or summary.confirmed = true)
        """)
    Optional<BidNoticeSummaryJpaEntity> findAccessible(
            @Param("companyId") Long companyId,
            @Param("summaryId") Long summaryId,
            @Param("userId") String userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select summary
        from BidNoticeSummaryJpaEntity summary
        where summary.summaryId = :summaryId
          and summary.companyId = :companyId
          and summary.requestedBy = :userId
          and summary.deletedAt is null
        """)
    Optional<BidNoticeSummaryJpaEntity> findOwnedForUpdate(
            @Param("companyId") Long companyId,
            @Param("summaryId") Long summaryId,
            @Param("userId") String userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select summary
        from BidNoticeSummaryJpaEntity summary
        where summary.summaryId = :summaryId
          and summary.companyId = :companyId
          and summary.noticeId = :noticeId
          and summary.requestedBy = :requestedBy
          and summary.deletedAt is null
        """)
    Optional<BidNoticeSummaryJpaEntity> findImprovementBaseForUpdate(
            @Param("companyId") Long companyId,
            @Param("noticeId") Long noticeId,
            @Param("requestedBy") String requestedBy,
            @Param("summaryId") Long summaryId
    );

    Optional<BidNoticeSummaryJpaEntity> findBySummaryIdAndDeletedAtIsNull(
            Long summaryId
    );

    // 정상 흐름에서는 요약과 outbox가 같은 트랜잭션에 저장돼 나오지 않아야 하는 상태다 - 그래도
    // 살아있는(PENDING) outbox가 하나도 없이 오래 멈춘 요약을 방어적으로 찾아 재발행 스케줄러가 훑는다.
    @Query(value = """
            SELECT s.bid_notice_summary_id
            FROM bid_notice_summary s
            WHERE s.summary_status IN ('PENDING', 'PROCESSING')
              AND s.deleted_at IS NULL
              AND s.updated_at < :staleBefore
              AND NOT EXISTS (
                  SELECT 1 FROM bid_notice_summary_outbox o
                  WHERE o.bid_notice_summary_id = s.bid_notice_summary_id
                    AND o.publish_status = 'PENDING'
              )
            ORDER BY s.bid_notice_summary_id
            LIMIT :batchLimit
            """, nativeQuery = true)
    List<Long> findOrphanedCandidateIds(
            @Param("staleBefore") LocalDateTime staleBefore,
            @Param("batchLimit") int batchLimit
    );
}
