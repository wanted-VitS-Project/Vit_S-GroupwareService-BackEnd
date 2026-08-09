package com.group3.vitamins.file.presentation.api.response;

import com.group3.vitamins.file.application.result.FileRestoreResult;
import io.swagger.v3.oas.annotations.media.Schema;

/** 휴지통 복구 응답(§6). */
public record FileRestoreResponse(
        @Schema(description = "복구된 문서 번호", example = "31")
        Long fileId,
        @Schema(description = "문서 표시명", example = "제안서")
        String name,
        @Schema(description = "복구된 블록 번호. 원래 블록이 삭제됐으면 null", example = "12")
        Long blockId,
        @Schema(description = "원래 블록이 삭제된 상태인지(true 면 프로젝트 문서함으로 복구)", example = "false")
        boolean blockDeleted
) {
    public static FileRestoreResponse from(FileRestoreResult result) {
        return new FileRestoreResponse(
                result.fileId(), result.name(), result.blockId(), result.blockDeleted());
    }
}
