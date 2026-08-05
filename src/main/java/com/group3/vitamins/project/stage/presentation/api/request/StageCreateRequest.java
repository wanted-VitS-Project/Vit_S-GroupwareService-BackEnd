package com.group3.vitamins.project.stage.presentation.api.request;

import com.group3.vitamins.project.stage.application.command.CreateStageCommand;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "스테이지 생성 요청")
public record StageCreateRequest(

        @Schema(description = "스테이지명 (최대 100자)", example = "제안")
        String name,

        @Schema(description = "정렬 순서. 생략하면 기존 최대값 + 1", example = "null")
        Integer sortOrder
) {

    /** 요청을 서비스 커맨드로 옮긴다. projectId 는 경로, requesterUserId·role 은 세션에서 온다. */
    public CreateStageCommand toCommand(Long projectId, String requesterUserId, String role) {
        return new CreateStageCommand(projectId, name, sortOrder, requesterUserId, role);
    }
}