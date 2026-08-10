package com.group3.vitamins.file.presentation.api.response;

import com.group3.vitamins.file.application.result.ProjectTrashFileResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.format.DateTimeFormatter;

@Schema(description = "프로젝트 휴지통 모아보기 항목(§13). 삭제(휴지통)된 문서 1건 + 스텝·블록 위치 + 휴지통 진입 시각. "
        + "블록도 삭제된 고아 파일이면 blockId·blockTitle 이 null 이고 blockDeleted=true.")
public record ProjectTrashFileResponse(
        @Schema(description = "삭제 당시 매달렸던 블록의 스텝 번호") Long stepId,
        @Schema(description = "스텝 표시명") String stepName,
        @Schema(description = "블록 번호. 블록도 삭제됐으면 null") Long blockId,
        @Schema(description = "블록 제목. 블록 삭제 시 null") String blockTitle,
        @Schema(description = "원래 블록이 삭제됐는지(고아 파일)") boolean blockDeleted,
        @Schema(description = "문서 번호") Long fileId,
        @Schema(description = "문서 표시명") String name,
        @Schema(description = "전체 버전 수") int versionCount,
        @Schema(description = "최신 버전 원본 파일명") String originalFileName,
        @Schema(description = "확장자") String extension,
        @Schema(description = "최신 버전 크기(바이트)") long sizeBytes,
        @Schema(description = "휴지통 진입 시각") String deletedAt
) {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static ProjectTrashFileResponse from(ProjectTrashFileResult r) {
        return new ProjectTrashFileResponse(
                r.stepId(), r.stepName(), r.blockId(), r.blockTitle(), r.blockDeleted(),
                r.fileId(), r.name(), r.versionCount(),
                r.originalFileName(), r.extension(), r.sizeBytes(),
                r.deletedAt() == null ? null : r.deletedAt().format(FMT));
    }
}
