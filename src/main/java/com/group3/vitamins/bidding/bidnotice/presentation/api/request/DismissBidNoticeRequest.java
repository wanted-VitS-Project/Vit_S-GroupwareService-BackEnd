package com.group3.vitamins.bidding.bidnotice.presentation.api.request;

import com.group3.vitamins.bidding.bidnotice.application.command.DismissBidNoticeCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DismissBidNoticeRequest(
        @Schema(
                description = "입찰 공고 제외 사유",
                example = "현재 회사의 사업 범위와 맞지 않는 공고입니다."
        )
        @NotBlank(message = "BIDDING_INVALID_DISMISS_REASON|입찰 공고 제외 사유를 입력해 주세요.")
        @Size(max = 500, message = "BIDDING_INVALID_DISMISS_REASON|입찰 공고 제외 사유는 500자를 넘을 수 없습니다.")
        String reason
) {

    public DismissBidNoticeCommand toCommand(
            Long noticeId,
            String userId,
            String role
    ) {
        return new DismissBidNoticeCommand(noticeId, reason, userId, role);
    }
}
