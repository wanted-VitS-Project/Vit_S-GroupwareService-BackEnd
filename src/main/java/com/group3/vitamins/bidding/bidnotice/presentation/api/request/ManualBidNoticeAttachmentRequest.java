package com.group3.vitamins.bidding.bidnotice.presentation.api.request;

import com.group3.vitamins.bidding.bidnotice.domain.model.ManualBidNoticeAttachment;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ManualBidNoticeAttachmentRequest(

        @NotBlank @Size(max = 255)
        @Schema(description = "첨부파일 표시명", example = "제안요청서.pdf")
        String fileName,

        @NotBlank @Size(max = 1000)
        @Pattern(regexp = "https?://.+")
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
