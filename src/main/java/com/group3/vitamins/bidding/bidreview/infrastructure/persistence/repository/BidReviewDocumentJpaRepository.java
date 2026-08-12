package com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository;

import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewDocumentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BidReviewDocumentJpaRepository
        extends JpaRepository<BidReviewDocumentJpaEntity, Long> {

    List<BidReviewDocumentJpaEntity>
    findAllByReviewIdOrderByReviewDocumentIdAsc(Long reviewId);
}