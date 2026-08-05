package com.group3.vitamins.activitylog.presentation.api.response;

import com.group3.vitamins.activitylog.application.result.ActivityLogResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record ActivityLogActorResponse(

        @Schema(description = "사용자 사번", example = "EMP003")
        String userId,

        @Schema(description = "사용자 이름", example = "이영희")
        String name
) {

    public static ActivityLogActorResponse from(ActivityLogResult.Actor actor) {
        return new ActivityLogActorResponse(
                actor.userId(),
                actor.name()
        );
    }
}
