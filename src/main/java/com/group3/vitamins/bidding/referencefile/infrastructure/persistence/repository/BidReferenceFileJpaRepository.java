package com.group3.vitamins.bidding.referencefile.infrastructure.persistence.repository;

import com.group3.vitamins.bidding.referencefile.infrastructure.persistence.entity.BidReferenceFileJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BidReferenceFileJpaRepository
        extends JpaRepository<BidReferenceFileJpaEntity, Long> {

    Optional<BidReferenceFileJpaEntity> findByReferenceFileIdAndCompanyIdAndDeletedAtIsNull(
            Long referenceFileId,
            Long companyId
    );

    // 검토 생성이 참조하는 사이 삭제되지 못하도록 행을 잠급니다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from BidReferenceFileJpaEntity f "
            + "where f.referenceFileId = :referenceFileId "
            + "and f.companyId = :companyId "
            + "and f.deletedAt is null")
    Optional<BidReferenceFileJpaEntity> findByReferenceFileIdAndCompanyIdAndDeletedAtIsNullForUpdate(
            @Param("referenceFileId") Long referenceFileId,
            @Param("companyId") Long companyId
    );

    List<BidReferenceFileJpaEntity>
    findAllByCompanyIdAndDeletedAtIsNullOrderByCreatedAtDescReferenceFileIdDesc(
            Long companyId
    );
}