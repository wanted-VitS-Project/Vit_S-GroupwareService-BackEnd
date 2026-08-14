package com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.entity;

import com.group3.vitamins.bidding.collectionrun.application.model.CollectedBidNotice;
import com.group3.vitamins.bidding.bidnotice.domain.model.ManualBidNoticeAttachment;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "bid_notice_attachment",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_bid_notice_attachment_order",
                columnNames = {
                        "bid_notice_id",
                        "attachment_kind",
                        "attachment_order"
                }
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BidNoticeAttachmentJpaEntity {

    // 링크형(첨부 URL) 첨부의 attachment_kind. 업로드형(MANUAL_UPLOAD)과 순번 네임스페이스가
    // 겹치지 않도록 분리한다(UNIQUE(bid_notice_id, attachment_kind, attachment_order)).
    public static final String LINK_ATTACHMENT_KIND = "NOTICE_SPEC";
    public static final String UPLOAD_ATTACHMENT_KIND = "MANUAL_UPLOAD";

    private static final String UPLOAD_STATUS_UPLOADING = "UPLOADING";
    private static final String UPLOAD_STATUS_READY = "READY";
    private static final String UPLOAD_STATUS_FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bid_notice_attachment_id")
    private Long bidNoticeAttachmentId;

    @Column(name = "bid_notice_id", nullable = false)
    private Long bidNoticeId;

    @Column(name = "attachment_kind", nullable = false, length = 30)
    private String attachmentKind;

    @Column(name = "attachment_order", nullable = false)
    private Short attachmentOrder;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "storage_key", length = 1000)
    private String storageKey;

    @Column(name = "upload_status", nullable = false, length = 20)
    private String uploadStatus;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 처음 확인된 공고 첨부파일을 생성합니다.
    public static BidNoticeAttachmentJpaEntity create(
            Long bidNoticeId,
            CollectedBidNotice.Attachment attachment,
            LocalDateTime now
    ) {
        BidNoticeAttachmentJpaEntity entity =
                new BidNoticeAttachmentJpaEntity();

        entity.bidNoticeId = bidNoticeId;
        entity.attachmentKind = LINK_ATTACHMENT_KIND;
        entity.attachmentOrder = convertOrder(attachment.order());
        entity.fileName = attachment.fileName();
        entity.sourceUrl = attachment.sourceUrl();
        entity.uploadStatus = UPLOAD_STATUS_READY;
        entity.createdAt = now;
        return entity;
    }

    // 직접 등록 공고의 공개 첨부 링크를 생성합니다.
    public static BidNoticeAttachmentJpaEntity createManual(
            Long bidNoticeId,
            ManualBidNoticeAttachment attachment,
            LocalDateTime now
    ) {
        BidNoticeAttachmentJpaEntity entity =
                new BidNoticeAttachmentJpaEntity();
        entity.bidNoticeId = bidNoticeId;
        entity.attachmentKind = LINK_ATTACHMENT_KIND;
        entity.attachmentOrder = convertOrder(attachment.attachmentOrder());
        entity.fileName = attachment.fileName();
        entity.sourceUrl = attachment.sourceUrl();
        entity.uploadStatus = UPLOAD_STATUS_READY;
        entity.createdAt = now;
        return entity;
    }

    // 직접 등록 공고의 업로드 대상 첨부를 UPLOADING 상태로 생성합니다. attachmentOrder는
    // UPLOAD_ATTACHMENT_KIND 네임스페이스 안에서 호출자가 다음 순번을 계산해 넘긴다.
    public static BidNoticeAttachmentJpaEntity createPendingUpload(
            Long bidNoticeId,
            int attachmentOrder,
            String fileName,
            String storageKey,
            long sizeBytes,
            String mimeType,
            LocalDateTime now
    ) {
        BidNoticeAttachmentJpaEntity entity = new BidNoticeAttachmentJpaEntity();
        entity.bidNoticeId = bidNoticeId;
        entity.attachmentKind = UPLOAD_ATTACHMENT_KIND;
        entity.attachmentOrder = convertOrder(attachmentOrder);
        entity.fileName = fileName;
        entity.storageKey = storageKey;
        entity.uploadStatus = UPLOAD_STATUS_UPLOADING;
        entity.sizeBytes = sizeBytes;
        entity.mimeType = mimeType;
        entity.createdAt = now;
        return entity;
    }

    // 외부 첨부파일 순서를 DB SMALLINT 범위로 변환합니다.
    private static short convertOrder(int order) {
        if (order <= 0 || order > Short.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "attachment order is out of SMALLINT range: " + order
            );
        }

        return (short) order;
    }

    // 같은 순서의 첨부파일 정보가 변경되면 최신 원문 정보로 갱신합니다.
    public void updateFrom(
            CollectedBidNotice.Attachment attachment,
            LocalDateTime now
    ) {
        this.fileName = attachment.fileName();
        this.sourceUrl = attachment.sourceUrl();
        this.updatedAt = now;
        this.deletedAt = null;
    }

    // 같은 순번의 직접 등록 첨부 링크를 최신 요청값으로 갱신합니다.
    public void updateManual(
            ManualBidNoticeAttachment attachment,
            LocalDateTime now
    ) {
        this.fileName = attachment.fileName();
        this.sourceUrl = attachment.sourceUrl();
        this.updatedAt = now;
        this.deletedAt = null;
    }
    public boolean isUploading() {
        return UPLOAD_STATUS_UPLOADING.equals(this.uploadStatus);
    }

    // 저장소 HEAD 검증까지 끝난 업로드를 완료 상태로 반영합니다. UPLOADING이 아니면 거부합니다.
    public void completeUpload(long verifiedSizeBytes, LocalDateTime now) {
        if (!isUploading()) {
            throw new IllegalStateException("UPLOADING 상태의 업로드만 완료 처리할 수 있습니다.");
        }
        this.uploadStatus = UPLOAD_STATUS_READY;
        this.sizeBytes = verifiedSizeBytes;
        this.updatedAt = now;
    }

    // 저장소에 객체가 없거나 크기가 다르면 실패로 종료합니다. 실패 전이는 재시도 대상이 아니다 -
    // 클라이언트가 새 업로드를 다시 시작해야 한다(파일 도메인 업로드 실패 처리와 동일 정책).
    public void failUpload(LocalDateTime now) {
        if (!isUploading()) {
            throw new IllegalStateException("UPLOADING 상태의 업로드만 실패 처리할 수 있습니다.");
        }
        this.uploadStatus = UPLOAD_STATUS_FAILED;
        this.updatedAt = now;
    }

    // 최신 공고 응답에서 사라진 첨부파일을 논리 삭제합니다.
    public void softDelete(LocalDateTime now) {
        if (this.deletedAt != null) {
            return;
        }

        this.deletedAt = now;
        this.updatedAt = now;
    }

}
