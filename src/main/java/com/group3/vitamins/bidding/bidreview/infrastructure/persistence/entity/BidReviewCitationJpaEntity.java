package com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Worker callback의 citations[]를 그대로 저장하는 append-only 스냅샷이다.
// 수정·삭제 없음 — 검토 하나당 한 번, COMPLETED 시점에만 생성된다(bid.md §Python 입찰 문서 검토 결과 callback).
@Getter
@Entity
@Table(name = "bid_review_citation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BidReviewCitationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bid_review_citation_id")
    private Long citationId;

    @Column(name = "bid_review_id", nullable = false, updatable = false)
    private Long reviewId;

    @Column(name = "bid_review_document_id", nullable = false, updatable = false)
    private Long reviewDocumentId;

    @Column(name = "rank_order", nullable = false, updatable = false)
    private int rankOrder;

    @Column(name = "file_name", nullable = false, length = 500, updatable = false)
    private String fileName;

    @Column(name = "page_number", updatable = false)
    private Integer pageNumber;

    @Column(name = "sheet_name", length = 255, updatable = false)
    private String sheetName;

    @Column(name = "excerpt", nullable = false, length = 1000, updatable = false)
    private String excerpt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static BidReviewCitationJpaEntity create(
            Long reviewId,
            Long reviewDocumentId,
            int rankOrder,
            String fileName,
            Integer pageNumber,
            String sheetName,
            String excerpt,
            LocalDateTime now
    ) {
        BidReviewCitationJpaEntity entity = new BidReviewCitationJpaEntity();
        entity.reviewId = reviewId;
        entity.reviewDocumentId = reviewDocumentId;
        entity.rankOrder = rankOrder;
        entity.fileName = fileName;
        entity.pageNumber = pageNumber;
        entity.sheetName = sheetName;
        entity.excerpt = excerpt;
        entity.createdAt = now;
        return entity;
    }
}
