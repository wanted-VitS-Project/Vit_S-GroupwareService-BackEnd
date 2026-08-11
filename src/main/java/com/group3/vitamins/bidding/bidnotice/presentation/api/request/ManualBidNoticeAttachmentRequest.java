package com.group3.vitamins.bidding.bidnotice.presentation.api.request;

import com.group3.vitamins.bidding.bidnotice.domain.model.ManualBidNoticeAttachment;
import io.swagger.v3.oas.annotations.media.Schema;

public record ManualBidNoticeAttachmentRequest(

        @Schema(description = "첨부파일 표시명", example = "제안요청서.pdf")
        String fileName,

        @Schema(
                description = "첨부파일 공개 원문 URL",
                example = "https://example.org/notices/2026-001/rfp.pdf"
        )
        String sourceUrl
) {

    // 요청 배열 순서를 첨부 순번으로 사용하여 도메인 입력값으로 변환합니다.
    public ManualBidNoticeAttachment toDomain(int attachmentOrder) {
        return new ManualBidNoticeAttachment(
                attachmentOrder,
                fileName,
                sourceUrl
        );
    }
}