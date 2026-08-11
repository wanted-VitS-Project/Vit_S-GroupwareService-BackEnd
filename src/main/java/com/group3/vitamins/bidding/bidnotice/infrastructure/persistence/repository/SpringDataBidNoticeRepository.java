package com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.repository;

import com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.entity.BidNoticeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SpringDataBidNoticeRepository
        extends JpaRepository<BidNoticeJpaEntity, Long> {

    // 공고 고유키로 기존 공고 한 건을 조회합니다.
    Optional<BidNoticeJpaEntity>
    findByCrawlSourceIdAndExternalIdAndNoticeOrder(
            Long crawlSourceId,
            String externalId,
            String noticeOrder
    );

    // 한 페이지의 공고를 한 번에 조회하여 공고별 N+1 조회를 줄입니다.
    List<BidNoticeJpaEntity>
    findAllByCrawlSourceIdAndExternalIdIn(
            Long crawlSourceId,
            Collection<String> externalIds
    );

    // 현재 회사가 소유한 활성 직접 등록 공고를 조회합니다.
    Optional<BidNoticeJpaEntity>
    findByBidNoticeIdAndOwnerCompanyIdAndCrawlSourceIdAndDeletedAtIsNull(
            Long bidNoticeId,
            Long ownerCompanyId,
            Long crawlSourceId
    );

    // 공용 외부 수집 공고인지 확인합니다.
    boolean existsByBidNoticeIdAndOwnerCompanyIdIsNullAndDeletedAtIsNull(
            Long bidNoticeId
    );

    // 등록 시 현재 회사에 같은 중복 키의 활성 공고가 있는지 확인합니다.
    boolean existsByOwnerCompanyIdAndManualDedupKeyAndDeletedAtIsNull(
            Long ownerCompanyId,
            String manualDedupKey
    );

    // 수정 대상을 제외하고 같은 중복 키의 활성 공고가 있는지 확인합니다.
    boolean existsByOwnerCompanyIdAndManualDedupKeyAndBidNoticeIdNotAndDeletedAtIsNull(
            Long ownerCompanyId,
            String manualDedupKey,
            Long excludedBidNoticeId
    );
}
