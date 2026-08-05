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
        String name
) {

    public RenameFileCommand toCommand(Long fileId, String requesterUserId, String role) {
        return new RenameFileCommand(fileId, name, requesterUserId, role);
    }
}
