package com.group3.vitamins.bidding.bidnotice.presentation.api.request;

import com.group3.vitamins.bidding.bidnotice.domain.model.ManualBidNoticeAttachment;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ManualBidNoticeAttachmentRequest(

        @NotBlank(message = "BIDDING_INVALID_MANUAL_NOTICE|첨부파일 표시명을 입력해 주세요.")
        @Size(max = 255, message = "BIDDING_INVALID_MANUAL_NOTICE|첨부파일 표시명은 255자를 넘을 수 없습니다.")
        @Schema(description = "첨부파일 표시명", example = "제안요청서.pdf")
        String fileName,

        @NotBlank(message = "BIDDING_INVALID_MANUAL_NOTICE|첨부파일 원문 URL을 입력해 주세요.")
        @Size(max = 1000, message = "BIDDING_INVALID_MANUAL_NOTICE|첨부파일 원문 URL은 1,000자를 넘을 수 없습니다.")
        @Pattern(
                regexp = "https?://.+",
                message = "BIDDING_INVALID_MANUAL_NOTICE|첨부파일 원문 URL 형식이 올바르지 않습니다. http:// 또는 https://로 시작해야 합니다."
        )
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
