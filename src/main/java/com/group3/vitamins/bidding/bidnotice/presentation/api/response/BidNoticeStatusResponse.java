package com.group3.vitamins.bidding.bidnotice.presentation.api.response;

import com.group3.vitamins.bidding.bidnotice.application.result.BidNoticeStatusResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record BidNoticeStatusResponse(
        @Schema(description = "입찰 공고 ID", example = "1")
        Long noticeId,

        @Schema(description = "현재 회사의 공고 상태", example = "DISMISSED")
        String noticeStatus,

        @Schema(
                description = "제외 사유. 복구 상태이면 null",
                example = "현재 회사의 사업 범위와 맞지 않는 공고입니다.",
                nullable = true
        )
        String dismissReason,

        @Schema(description = "현재 회사의 관심 등록 여부(회사 공용)", example = "false")
        boolean isFavorite,

        @Schema(description = "상태 변경 시각", example = "2026-08-11T16:00:00")
        LocalDateTime updatedAt
) {

    public static BidNoticeStatusResponse from(BidNoticeStatusResult result) {
        return new BidNoticeStatusResponse(
                result.noticeId(),
                result.noticeStatus(),
                result.dismissReason(),
                result.isFavorite(),
                result.updatedAt()
        );
    }
}
