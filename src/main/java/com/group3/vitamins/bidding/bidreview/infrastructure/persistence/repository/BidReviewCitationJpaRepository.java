package com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository;

import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewCitationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidReviewCitationJpaRepository
        extends JpaRepository<BidReviewCitationJpaEntity, Long> {
}
