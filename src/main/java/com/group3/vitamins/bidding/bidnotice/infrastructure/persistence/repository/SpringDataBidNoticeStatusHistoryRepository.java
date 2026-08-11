package com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.repository;

import com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.entity.BidNoticeStatusHistoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataBidNoticeStatusHistoryRepository
        extends JpaRepository<BidNoticeStatusHistoryJpaEntity, Long> {
}
