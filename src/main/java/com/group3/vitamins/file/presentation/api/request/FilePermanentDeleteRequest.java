package com.group3.vitamins.file.presentation.api.request;

import com.group3.vitamins.file.application.command.PermanentDeleteFileCommand;
import io.swagger.v3.oas.annotations.media.Schema;

/** 영구 삭제 요청(§7). 확인 문자는 정확히 {@code "영구 삭제"} 여야 한다. */
public record FilePermanentDeleteRequest(
        @Schema(description = "영구 삭제 확인 문자. 정확히 \"영구 삭제\" 여야 한다.", example = "영구 삭제")
        String confirmText
) {
    public PermanentDeleteFileCommand toCommand(Long fileId, String requesterUserId, String role) {
        return new PermanentDeleteFileCommand(fileId, confirmText, requesterUserId, role);
    }
}
