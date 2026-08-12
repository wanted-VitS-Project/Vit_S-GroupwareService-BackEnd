package com.group3.vitamins.bidding.bidreview.presentation.api.response;

import com.group3.vitamins.bidding.bidreview.application.result.BidReviewSourcesResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record BidReviewSourcesResponse(

        @Schema(description = "입찰 공고 ID", example = "1")
        Long noticeId,

        @Schema(description = "삭제되지 않은 공고 첨부 목록")
        List<AttachmentSourceResponse> attachments
) {

    public static BidReviewSourcesResponse from(BidReviewSourcesResult result) {
        return new BidReviewSourcesResponse(
                result.noticeId(),
                result.attachments().stream()
                        .map(AttachmentSourceResponse::from)
                        .toList()
        );
    }

    public record AttachmentSourceResponse(

            @Schema(description = "검토 요청에서 사용하는 첨부 ID", example = "31")
            Long attachmentId,

            @Schema(description = "화면 표시용 원본 파일명", example = "제안요청서.pdf")
            String fileName,

            @Schema(description = "수집 출처 코드", example = "NARA")
            String sourceType,

            @Schema(description = "현재 문서 추출 지원 형식 여부", example = "true")
            boolean supported
    ) {

        public static AttachmentSourceResponse from(
                BidReviewSourcesResult.AttachmentSourceResult result
        ) {
            return new AttachmentSourceResponse(
                    result.attachmentId(),
                    result.fileName(),
                    result.sourceType(),
                    result.supported()
            );
        }
    }
}