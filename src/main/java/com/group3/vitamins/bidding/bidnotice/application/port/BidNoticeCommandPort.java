package com.group3.vitamins.bidding.bidnotice.application.port;

import com.group3.vitamins.bidding.bidnotice.domain.model.ManualBidNotice;

import java.time.LocalDateTime;
import java.util.Optional;

// 직접 등록 공고의 조회, 중복 확인, 저장에 필요한 영속성 기능을 묶은 포트입니다.
public interface BidNoticeCommandPort {

    // 논리 삭제되지 않은 직접 등록 수집처의 ID를 조회합니다.
    Optional<Long> findManualSourceId();

    // 현재 회사가 소유한 직접 등록 공고를 수정 목적으로 조회합니다.
    Optional<ManualBidNotice> findOwnedManualNotice(Long companyId, Long noticeId);

    // 공용 외부 수집 공고인지 확인하여 직접 등록 공고와 다른 수정 오류를 반환할 수 있게 합니다.
    boolean existsExternalNotice(Long noticeId);

    // 현재 회사를 기준으로 같은 중복 키를 가진 다른 활성 공고가 있는지 확인합니다.
    boolean existsActiveDuplicate(
            Long companyId,
            String manualDedupKey,
            Long excludedNoticeId
    );

    // 직접 등록 공고와 첨부 링크를 하나의 저장 책임으로 반영합니다.
    ManualBidNotice save(ManualBidNotice notice);

    // 링크형+업로드형을 합친 현재 활성(삭제되지 않은) 첨부 총 개수 - 최대 개수 제한 검증용.
    long countActiveAttachments(Long noticeId);

    // 업로드 대상 첨부를 UPLOADING 상태로 생성합니다.
    PendingAttachmentUpload createPendingUpload(
            Long noticeId,
            String fileName,
            String storageKey,
            long sizeBytes,
            String mimeType,
            LocalDateTime now
    );

    // 완료 확인 대상 업로드를 조회합니다. UPLOADING 상태인지는 호출자가 검증합니다.
    Optional<PendingAttachmentUpload> findPendingUpload(Long noticeId, Long attachmentId);

    // 저장소 HEAD 검증까지 끝난 업로드를 완료 상태로 반영합니다.
    void completeUpload(Long attachmentId, long verifiedSizeBytes, LocalDateTime now);

    // 저장소에 객체가 없거나 크기가 다른 업로드를 실패로 종료합니다. 호출부의 @Transactional이
    // 이후 예외로 롤백되더라도 이 실패 기록만은 남아야 하므로 REQUIRES_NEW로 별도 커밋한다.
    void failUploadInNewTransaction(Long attachmentId, LocalDateTime now);

    record PendingAttachmentUpload(
            Long attachmentId,
            String fileName,
            String storageKey,
            long sizeBytes,
            boolean uploading
    ) {
    }
}
