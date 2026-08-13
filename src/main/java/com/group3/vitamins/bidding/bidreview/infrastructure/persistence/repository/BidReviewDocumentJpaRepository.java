package com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository;

import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewDocumentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BidReviewDocumentJpaRepository
        extends JpaRepository<BidReviewDocumentJpaEntity, Long> {

    List<BidReviewDocumentJpaEntity>
    findAllByReviewIdOrderByDocumentRoleAscReviewDocumentIdAsc(Long reviewId);

    // Worker callback의 documents[] 진행상황 갱신 대상을 찾는다 (BID_ATTACHMENT 역할만 해당).
    Optional<BidReviewDocumentJpaEntity>
    findByReviewIdAndBidAttachmentId(Long reviewId, Long bidAttachmentId);

    // Worker callback의 citations[]가 가리키는 근거 문서(사내 기준자료)를 찾는다.
    Optional<BidReviewDocumentJpaEntity>
    findByReviewIdAndReferenceFileId(Long reviewId, Long referenceFileId);
}