package com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.repository;

import com.group3.vitamins.bidding.bidsummary.domain.model.BidNoticeSummaryStatus;
import com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.entity.BidNoticeSummaryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
