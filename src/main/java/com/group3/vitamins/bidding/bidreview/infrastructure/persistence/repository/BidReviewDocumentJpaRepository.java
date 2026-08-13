package com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository;

import com.group3.vitamins.bidding.bidreview.domain.model.BidReviewDocumentRole;
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

    // Worker callback의 citations[]가 가리키는 근거 문서를 찾는다. documentRole도 함께 걸어서
    // 식별자 값이 다른 역할의 컬럼과 우연히 겹쳐도(예: bidAttachmentId=5와 companyDocumentVersionId=5)
    // 잘못된 문서로 연결되지 않게 한다(CodeRabbit 2026-08-13 피드백).
    Optional<BidReviewDocumentJpaEntity>
    findByReviewIdAndDocumentRoleAndBidAttachmentId(
            Long reviewId, BidReviewDocumentRole documentRole, Long bidAttachmentId);

    Optional<BidReviewDocumentJpaEntity>
    findByReviewIdAndDocumentRoleAndReferenceFileId(
            Long reviewId, BidReviewDocumentRole documentRole, Long referenceFileId);

    Optional<BidReviewDocumentJpaEntity>
    findByReviewIdAndDocumentRoleAndCompanyDocumentVersionId(
            Long reviewId, BidReviewDocumentRole documentRole, Long companyDocumentVersionId);

    List<BidReviewDocumentJpaEntity> findAllByReviewIdAndDocumentRoleAndDeletedAtIsNull(
            Long reviewId,
            BidReviewDocumentRole documentRole
    );
}