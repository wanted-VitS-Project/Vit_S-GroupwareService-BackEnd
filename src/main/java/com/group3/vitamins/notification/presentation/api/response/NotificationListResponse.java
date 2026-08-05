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
                clampToInt(result.totalElements()),
                result.totalPages());
    }

    /** API 명세상 totalElements 타입은 int 로 고정이라(`.ai/api/notification.md`), long→int 는 오버플로 대신 최댓값으로 자른다. */
    private static int clampToInt(long value) {
        return (int) Math.min(value, Integer.MAX_VALUE);
    }
}
