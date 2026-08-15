package com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.repository;

import com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.entity.BidNoticeAttachmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    // UPLOADING 상태일 때만 원자적으로 완료 반영한다. 영향 행이 0이면 동시에 다른 완료/실패 통보가
    // 먼저 반영된 것이므로 호출부가 ALREADY_COMPLETED로 변환한다.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE BidNoticeAttachmentJpaEntity a SET a.uploadStatus = 'READY', "
            + "a.sizeBytes = :sizeBytes, a.updatedAt = :now "
            + "WHERE a.bidNoticeAttachmentId = :id AND a.uploadStatus = 'UPLOADING'")
    int completeUploadIfPending(
            @Param("id") Long id,
            @Param("sizeBytes") long sizeBytes,
            @Param("now") LocalDateTime now
    );

    // UPLOADING 상태일 때만 원자적으로 실패 반영한다. 이미 종료된 상태면 조용히 0건으로 끝난다 -
    // 실패 기록은 최선노력(best-effort) 보조 처리라 호출부가 별도 예외를 던지지 않는다.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE BidNoticeAttachmentJpaEntity a SET a.uploadStatus = 'FAILED', a.updatedAt = :now "
            + "WHERE a.bidNoticeAttachmentId = :id AND a.uploadStatus = 'UPLOADING'")
    int failUploadIfPending(@Param("id") Long id, @Param("now") LocalDateTime now);
}
