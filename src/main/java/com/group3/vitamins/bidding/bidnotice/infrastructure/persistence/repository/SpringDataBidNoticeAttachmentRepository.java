package com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.repository;

import com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.entity.BidNoticeAttachmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SpringDataBidNoticeAttachmentRepository
        extends JpaRepository<BidNoticeAttachmentJpaEntity, Long> {

    // 여러 공고의 삭제 이력을 포함한 첨부파일을 한 번에 조회합니다.
    List<BidNoticeAttachmentJpaEntity>
    findAllByBidNoticeIdIn(
            Collection<Long> bidNoticeIds
    );
}
