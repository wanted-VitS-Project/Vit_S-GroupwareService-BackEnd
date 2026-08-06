package com.group3.vitamins.notification.presentation.api.response;

import com.group3.vitamins.notification.application.result.NotificationTargetResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "알림 이동 대상 조회 응답")
public record NotificationTargetResponse(

        @Schema(description = "이동 대상 도메인 유형. 매핑 없으면 NONE", example = "APPROVAL")
        String type,

        @Schema(description = "이동 대상 구분 번호. type=NONE 이면 null", example = "55")
        Long targetId,

        @Schema(description = "도메인별 부가 정보. 결재면 revisionId")
        Map<String, Object> extra
) {

    public static NotificationTargetResponse from(NotificationTargetResult result) {
        return new NotificationTargetResponse(result.type(), result.targetId(), result.extra());
    }
}
