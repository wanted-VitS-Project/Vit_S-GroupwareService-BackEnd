package com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository;

import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewOutboxJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidReviewOutboxJpaRepository
        extends JpaRepository<BidReviewOutboxJpaEntity, Long> {
}