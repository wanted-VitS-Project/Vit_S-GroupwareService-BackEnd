package com.group3.vitamins.bidding.bidreview.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BidReviewDocument 프로젝트 파일 귀속(promote)")
class BidReviewDocumentTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 13, 9, 0);

    @Test
    @DisplayName("READY 상태의 공고 첨부는 귀속 정보를 반영하고 PROMOTED로 전이한다")
    void promotesReadyBidAttachment() {
        BidReviewDocument document = BidReviewDocument.createBidAttachment(31L, "제안요청서.pdf", NOW)
                .ready("temp-key", 1024L, "application/pdf", NOW);

        BidReviewDocument promoted = document.promote(501L, 9001L, NOW.plusMinutes(5));

        assertThat(promoted.processingStatus()).isEqualTo(BidReviewDocumentStatus.PROMOTED);
        assertThat(promoted.promotedFileId()).isEqualTo(501L);
        assertThat(promoted.promotedFileVersionId()).isEqualTo(9001L);
        assertThat(promoted.promotedAt()).isEqualTo(NOW.plusMinutes(5));
        // 임시 저장소 키·크기 등 기존 스냅샷은 그대로 남는다 - 정리(cleanup)는 별도 절차.
        assertThat(promoted.temporaryStorageKey()).isEqualTo("temp-key");
    }

    @Test
    @DisplayName("사내 기준자료(INTERNAL_REFERENCE)는 귀속 대상이 아니다")
    void rejectsNonBidAttachmentRole() {
        BidReviewDocument document = BidReviewDocument.createInternalReference(501L, "원가계산_기준.pdf", NOW);

        assertThatThrownBy(() -> document.promote(501L, 9001L, NOW))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("다운로드가 끝나지 않은(READY가 아닌) 공고 첨부는 귀속할 수 없다")
    void rejectsNotReadyBidAttachment() {
        BidReviewDocument document = BidReviewDocument.createBidAttachment(31L, "제안요청서.pdf", NOW);

        assertThatThrownBy(() -> document.promote(501L, 9001L, NOW))
                .isInstanceOf(IllegalStateException.class);
    }
}
