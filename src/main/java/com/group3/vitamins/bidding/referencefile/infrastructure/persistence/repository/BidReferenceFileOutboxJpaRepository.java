package com.group3.vitamins.bidding.referencefile.infrastructure.persistence.repository;

import com.group3.vitamins.bidding.referencefile.infrastructure.persistence.entity.BidReferenceFileOutboxJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidReferenceFileOutboxJpaRepository
        extends JpaRepository<BidReferenceFileOutboxJpaEntity, Long> {
}