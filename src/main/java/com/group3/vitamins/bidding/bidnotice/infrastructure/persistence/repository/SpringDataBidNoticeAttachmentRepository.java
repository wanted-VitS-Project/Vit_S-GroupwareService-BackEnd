package com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.repository;

import com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.entity.BidNoticeAttachmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SpringDataBidNoticeAttachmentRepository
        extends JpaRepository<BidNoticeAttachmentJpaEntity, Long> {

    // 여러 공고의 삭제 이력을 포함한 첨부파일을 한 번에 조회합니다.
    List<BidNoticeAttachmentJpaEntity>
    findAllByBidNoticeIdIn(
            Collection<Long> bidNoticeIds
    );

    // 직접 등록 공고의 삭제 이력을 포함한 첨부 링크를 순번대로 조회합니다.
    // ⚠️ attachmentKind로 스코프한다 - 업로드형(MANUAL_UPLOAD) 첨부가 이 순번 diff 대상에 섞이면
    // 링크형 PATCH 동기화가 업로드 첨부를 잘못 갱신·삭제할 수 있다.
    List<BidNoticeAttachmentJpaEntity>
    findAllByBidNoticeIdAndAttachmentKindOrderByAttachmentOrder(Long bidNoticeId, String attachmentKind);

    // 같은 공고·같은 종류 첨부 중 현재까지 부여된 최대 순번을 조회합니다(업로드 순번 채번용).
    Optional<BidNoticeAttachmentJpaEntity>
    findFirstByBidNoticeIdAndAttachmentKindOrderByAttachmentOrderDesc(Long bidNoticeId, String attachmentKind);

    // 링크형+업로드형을 합친 현재 활성(삭제되지 않은) 첨부 총 개수 - 최대 개수 제한 검증용.
    long countByBidNoticeIdAndDeletedAtIsNull(Long bidNoticeId);

    // 업로드 완료 확인 대상을 같은 공고 소속·업로드형으로 한정해 조회합니다.
    Optional<BidNoticeAttachmentJpaEntity>
    findByBidNoticeAttachmentIdAndBidNoticeIdAndAttachmentKind(
            Long bidNoticeAttachmentId, Long bidNoticeId, String attachmentKind
    );
}
