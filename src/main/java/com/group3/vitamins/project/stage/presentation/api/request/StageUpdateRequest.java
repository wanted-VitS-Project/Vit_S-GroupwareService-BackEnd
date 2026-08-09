package com.group3.vitamins.project.stage.presentation.api.request;

import com.group3.vitamins.project.stage.application.command.UpdateStageCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 스테이지 수정 요청. 이름만 바꾼다 — 순서는 순서 변경 API 소관이다. */
@Schema(description = "스테이지 수정 요청")
public record StageUpdateRequest(

        @NotBlank(message = "STAGE_NAME_REQUIRED|스테이지명을 입력해 주세요.")
        @Size(max = 100, message = "STAGE_NAME_TOO_LONG|스테이지명은 100자를 넘을 수 없습니다.")
        @Schema(description = "스테이지명 (최대 100자)", example = "제안·계약")
        String name
) {

    public UpdateStageCommand toCommand(Long stageId, String requesterUserId, String role) {
        return new UpdateStageCommand(stageId, name, requesterUserId, role);
    }
}
