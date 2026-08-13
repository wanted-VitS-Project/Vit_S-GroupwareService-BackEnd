package com.group3.vitamins.bidding.referencefile.infrastructure.persistence.repository;

import com.group3.vitamins.bidding.referencefile.infrastructure.persistence.entity.BidReferenceFileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BidReferenceFileJpaRepository
        extends JpaRepository<BidReferenceFileJpaEntity, Long> {

    Optional<BidReferenceFileJpaEntity> findByReferenceFileIdAndCompanyIdAndDeletedAtIsNull(
            Long referenceFileId,
            Long companyId
    );

    List<BidReferenceFileJpaEntity>
    findAllByCompanyIdAndDeletedAtIsNullOrderByCreatedAtDescReferenceFileIdDesc(
            Long companyId
    );
}