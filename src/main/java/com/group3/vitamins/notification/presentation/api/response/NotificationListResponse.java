package com.group3.vitamins.notification.presentation.api.response;

import com.group3.vitamins.notification.application.result.NotificationPageResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "알림 목록 응답")
public record NotificationListResponse(

        @Schema(description = "알림 목록(최신순)")
        List<NotificationResponse> content,

        @Schema(description = "해당 필터 기준 전체 개수(탭 숫자로 사용)", example = "1")
        int totalElements,

        @Schema(description = "전체 페이지 수", example = "1")
        int totalPages
) {

    public static NotificationListResponse from(NotificationPageResult result) {
        return new NotificationListResponse(
                result.content().stream().map(NotificationResponse::from).toList(),
                (int) result.totalElements(),
                result.totalPages());
    }
}
