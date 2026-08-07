package com.group3.vitamins.file.presentation.api.response;

import com.group3.vitamins.file.application.result.FilePermanentDeleteResult;
import io.swagger.v3.oas.annotations.media.Schema;

/** 영구 삭제 응답(§7). */
public record FilePermanentDeleteResponse(
        @Schema(description = "영구 삭제된 문서 번호", example = "31")
        Long fileId,
        @Schema(description = "삭제된 버전 수", example = "3")
        int deletedVersionCount,
        @Schema(description = "저장소에서 실제 삭제된 객체 수(일부 실패 시 버전 수보다 적을 수 있다)", example = "3")
        int storageDeletedCount
) {
    public static FilePermanentDeleteResponse from(FilePermanentDeleteResult result) {
        return new FilePermanentDeleteResponse(
                result.fileId(), result.deletedVersionCount(), result.storageDeletedCount());
    }
}
