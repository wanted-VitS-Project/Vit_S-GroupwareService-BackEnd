package com.group3.vitamins.file.presentation.api.request;

import com.group3.vitamins.file.application.command.RenameFileCommand;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 문서명 수정 요청(§4). 표시명만 바꾼다 — 원본 파일명은 버전에 남는다.
 * 길이·공백 검증은 서비스에서 하고 {@code FILE_INVALID_REQUEST} 로 응답한다(명세 이탈 방지).
 */
@Schema(description = "문서명 수정 요청. 표시명만 바꾼다.")
public record FileRenameRequest(
        @Schema(description = "새 문서 표시명(최대 255자)", example = "제안서_최종",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Schema(description = "조회에서 받은 낙관락 버전. 저장 조건으로 건다.", example = "3",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Integer version,

        @Schema(description = "true 면 충돌을 무시하고 덮어쓴다. 생략 시 false", example = "false",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Boolean overwrite
) {

    public RenameFileCommand toCommand(Long fileId, String requesterUserId, String role) {
        // version 누락(null)은 0 으로 넘겨 서비스가 400 으로 막는다(§6-3: WHERE version=0 → 전부 409 방지).
        return new RenameFileCommand(
                fileId, name,
                version == null ? 0 : version,
                overwrite != null && overwrite,
                requesterUserId, role);
    }
}
