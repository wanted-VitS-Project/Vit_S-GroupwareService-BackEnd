package com.group3.vitamins.bidding.bidreview.presentation.api.request;

import com.group3.vitamins.bidding.bidreview.application.command.CreateBidReviewCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateBidReviewRequest(

        @NotNull(message = "BIDDING_INVALID_REVIEW_REQUEST|검토할 공고 첨부파일을 선택해 주세요.")
        @Size(min = 1, max = 10, message = "BIDDING_INVALID_REVIEW_REQUEST|공고 첨부파일은 1개 이상 10개 이하로 선택해 주세요.")
        @Schema(description = "검토할 공고 첨부파일 ID 목록", example = "[31, 32]")
        List<@NotNull(message = "BIDDING_INVALID_REVIEW_REQUEST|공고 첨부파일 ID가 올바르지 않습니다.")
             @Positive(message = "BIDDING_INVALID_REVIEW_REQUEST|공고 첨부파일 ID가 올바르지 않습니다.")
             Long> bidAttachmentIds,

        @Size(max = 10, message = "BIDDING_INVALID_REVIEW_REQUEST|입찰 기준자료는 최대 10개까지 선택할 수 있습니다.")
        @Schema(description = "비교 기준으로 사용할 입찰 기준자료 ID 목록", example = "[501, 502]", nullable = true)
        List<@NotNull(message = "BIDDING_INVALID_REVIEW_REQUEST|입찰 기준자료 ID가 올바르지 않습니다.")
             @Positive(message = "BIDDING_INVALID_REVIEW_REQUEST|입찰 기준자료 ID가 올바르지 않습니다.")
             Long> referenceFileIds,

        @Size(max = 10, message = "BIDDING_INVALID_REVIEW_REQUEST|사내 문서함 참조는 최대 10개까지 선택할 수 있습니다.")
        @Schema(description = "비교 기준으로 사용할 사내 문서함 참조 버전 ID 목록", example = "[9001, 9002]", nullable = true)
        List<@NotNull(message = "BIDDING_INVALID_REVIEW_REQUEST|사내 문서함 참조 버전 ID가 올바르지 않습니다.")
             @Positive(message = "BIDDING_INVALID_REVIEW_REQUEST|사내 문서함 참조 버전 ID가 올바르지 않습니다.")
             Long> companyDocumentVersionIds,

        @NotBlank(message = "BIDDING_INVALID_REVIEW_REQUEST|검토 지시를 입력해 주세요.")
        @Size(max = 3000, message = "BIDDING_INVALID_REVIEW_REQUEST|검토 지시는 3,000자를 넘을 수 없습니다.")
        @Schema(
                description = "사용자가 직접 입력한 검토 지시",
                example = "우리 회사의 재정 상태와 보유 인력으로 수행 가능한지, 부족한 자격과 실적을 근거와 함께 검토해줘."
        )
        String prompt
) {

    public CreateBidReviewCommand toCommand(Long noticeId, String userId, String role) {
        return new CreateBidReviewCommand(
                noticeId, bidAttachmentIds, referenceFileIds, companyDocumentVersionIds, prompt, userId, role
        );
    }
}