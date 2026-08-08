package com.group3.vitamins.notification.presentation.api.response;

import com.group3.vitamins.notification.application.result.MarkNotificationReadResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "알림 읽음 처리 응답")
public record MarkNotificationReadResponse(

        @Schema(description = "읽음 처리된 알림 구분 번호", example = "301")
        Long notificationId,

        @Schema(description = "읽은 시각. 이미 읽은 알림이면 최초 읽음 시각이 그대로 반환된다")
        LocalDateTime readAt
) {

    public static MarkNotificationReadResponse from(MarkNotificationReadResult result) {
        return new MarkNotificationReadResponse(result.notificationId(), result.readAt());
    }
}
