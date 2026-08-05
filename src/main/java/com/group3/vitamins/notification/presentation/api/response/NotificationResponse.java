package com.group3.vitamins.notification.presentation.api.response;

import com.group3.vitamins.notification.application.result.NotificationResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "알림 목록 항목")
public record NotificationResponse(

        @Schema(description = "알림 구분 번호", example = "301")
        Long notificationId,

        @Schema(description = "연결된 블록. null 이면 block 과 무관한 알림", example = "101")
        Long blockId,

        @Schema(description = "알림 유형", example = "APPROVAL_REQUESTED")
        String notificationType,

        @Schema(description = "알림 제목", example = "결재 요청")
        String title,

        @Schema(description = "알림 내용", example = "출장비 정산 결재 요청이 도착했습니다.")
        String message,

        @Schema(description = "읽은 시각. null 이면 안 읽음")
        LocalDateTime readAt,

        @Schema(description = "생성 일시")
        LocalDateTime createdAt
) {

    public static NotificationResponse from(NotificationResult result) {
        return new NotificationResponse(
                result.notificationId(), result.blockId(), result.notificationType(),
                result.title(), result.message(), result.readAt(), result.createdAt());
    }
}
