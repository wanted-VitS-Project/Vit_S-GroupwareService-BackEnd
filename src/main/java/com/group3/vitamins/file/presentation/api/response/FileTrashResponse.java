package com.group3.vitamins.file.presentation.api.response;

import com.group3.vitamins.file.application.result.FileTrashResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "휴지통 이동 응답(§5). 저장소 객체는 유지되고 삭제 시각만 기록된다.")
public record FileTrashResponse(
        @Schema(description = "문서 id", example = "31") Long fileId,
        @Schema(description = "휴지통 진입 시각", example = "2026-08-06T09:00:00") LocalDateTime deletedAt
) {

    public static FileTrashResponse from(FileTrashResult r) {
        return new FileTrashResponse(r.fileId(), r.deletedAt());
    }
}
