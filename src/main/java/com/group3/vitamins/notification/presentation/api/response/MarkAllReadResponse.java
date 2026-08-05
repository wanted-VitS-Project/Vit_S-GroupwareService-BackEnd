package com.group3.vitamins.notification.presentation.api.response;

import com.group3.vitamins.notification.application.result.MarkAllReadResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "알림 전체 읽음 처리 응답")
public record MarkAllReadResponse(

        @Schema(description = "이번에 읽음 처리된 알림 개수", example = "5")
        int markedCount
) {

    public static MarkAllReadResponse from(MarkAllReadResult result) {
        return new MarkAllReadResponse(result.markedCount());
    }
}
