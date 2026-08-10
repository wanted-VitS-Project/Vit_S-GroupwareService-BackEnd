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
}