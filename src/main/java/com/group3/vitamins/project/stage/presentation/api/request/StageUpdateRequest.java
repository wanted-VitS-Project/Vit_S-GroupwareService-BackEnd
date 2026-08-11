package com.group3.vitamins.project.stage.presentation.api.request;

import com.group3.vitamins.project.stage.application.command.UpdateStageCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 스테이지 수정 요청. 이름만 바꾼다 — 순서는 순서 변경 API 소관이다. */
@Schema(description = "스테이지 수정 요청")
public record StageUpdateRequest(

        @NotBlank(message = "STAGE_NAME_REQUIRED|스테이지명을 입력해 주세요.")
        @Size(max = 100, message = "STAGE_NAME_TOO_LONG|스테이지명은 100자를 넘을 수 없습니다.")
        @Schema(description = "스테이지명 (최대 100자)", example = "제안·계약")
        String name,

        @NotNull(message = "STAGE_VERSION_REQUIRED|버전 정보가 없습니다. 화면을 새로고침해 주세요.")
        @Schema(description = "목록 조회에서 받은 version 을 그대로 실어 보낸다. "
                + "그 사이 남이 먼저 저장했으면 409 다", example = "7")
        Integer version,

        @Schema(description = "true 면 충돌을 무시하고 덮어쓴다. 생략하면 false", example = "false")
        Boolean overwrite
) {

    /** ⚠️ overwrite 는 선택 필드라 null 이 온다. {@code Boolean.TRUE.equals} 로 받아야 NPE 가 안 난다. */
    public UpdateStageCommand toCommand(Long stageId, String requesterUserId, String role) {
        return new UpdateStageCommand(
                stageId, name, version, Boolean.TRUE.equals(overwrite), requesterUserId, role);
    }
}
