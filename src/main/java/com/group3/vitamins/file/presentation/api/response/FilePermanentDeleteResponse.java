package com.group3.vitamins.file.presentation.api.response;

import com.group3.vitamins.file.application.result.FilePermanentDeleteResult;
import io.swagger.v3.oas.annotations.media.Schema;

/** 영구 삭제 응답(§7). */
public record FilePermanentDeleteResponse(
        @Schema(description = "영구 삭제된 문서 번호", example = "31")
        Long fileId,
        @Schema(description = "삭제된 버전 수", example = "3")
        int deletedVersionCount,
        @Schema(description = "저장소 삭제를 요청한 객체 수. 실제 삭제는 커밋 후 best-effort 로 수행된다(실패 키는 정리 대상)", example = "3")
        int storageDeletedCount
) {
    public static FilePermanentDeleteResponse from(FilePermanentDeleteResult result) {
        return new FilePermanentDeleteResponse(
                result.fileId(), result.deletedVersionCount(), result.storageDeletedCount());
    }
}
