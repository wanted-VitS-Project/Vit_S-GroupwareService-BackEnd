package com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.repository;

import com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.entity.BidNoticeRawJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SpringDataBidNoticeRawRepository
        extends JpaRepository<BidNoticeRawJpaEntity, Long> {

    // 여러 공고에서 요청한 해시와 일치하는 원문 식별자만 조회합니다.
    @Query("""
            SELECT
                raw.bidNoticeId AS bidNoticeId,
                raw.rawPayloadHash AS rawPayloadHash
            FROM BidNoticeRawJpaEntity raw
            WHERE raw.bidNoticeId IN :bidNoticeIds
              AND raw.rawPayloadHash IN :rawPayloadHashes
              AND raw.deletedAt IS NULL
            """)
    List<ExistingRawKey> findExistingRawKeys(
            @Param("bidNoticeIds") Collection<Long> bidNoticeIds,
            @Param("rawPayloadHashes") Collection<String> rawPayloadHashes
    );

    interface ExistingRawKey {

        Long getBidNoticeId();

        String getRawPayloadHash();
    }
}