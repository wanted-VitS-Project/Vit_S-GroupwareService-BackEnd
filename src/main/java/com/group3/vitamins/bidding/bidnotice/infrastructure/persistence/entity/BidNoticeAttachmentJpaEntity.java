package com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.entity;

import com.group3.vitamins.bidding.collectionrun.application.model.CollectedBidNotice;
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

    private static final String DEFAULT_ATTACHMENT_KIND = "NOTICE_SPEC";

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

    @Column(name = "source_url", nullable = false, length = 1000)
    private String sourceUrl;

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
        entity.attachmentKind = DEFAULT_ATTACHMENT_KIND;
        entity.attachmentOrder = convertOrder(attachment.order());
        entity.fileName = attachment.fileName();
        entity.sourceUrl = attachment.sourceUrl();
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
    // 최신 공고 응답에서 사라진 첨부파일을 논리 삭제합니다.
    public void softDelete(LocalDateTime now) {
        if (this.deletedAt != null) {
            return;
        }

        this.deletedAt = now;
        this.updatedAt = now;
    }

}