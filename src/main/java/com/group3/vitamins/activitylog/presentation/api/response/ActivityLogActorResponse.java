package com.group3.vitamins.activitylog.presentation.api.response;

import com.group3.vitamins.activitylog.application.result.ActivityLogResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record ActivityLogActorResponse(

        @Schema(description = "사용자 사번", example = "EMP003")
        String userId,

        @Schema(description = "사용자 이름", example = "이영희")
        String name,

        @Schema(description = "퇴사일. 재직 중이면 null", example = "2026-08-01", nullable = true)
        LocalDate resignedAt
) {

    public static ActivityLogActorResponse from(ActivityLogResult.Actor actor) {
        return new ActivityLogActorResponse(
                actor.userId(),
                actor.name(),
                actor.resignedAt()
        );
    }
}
