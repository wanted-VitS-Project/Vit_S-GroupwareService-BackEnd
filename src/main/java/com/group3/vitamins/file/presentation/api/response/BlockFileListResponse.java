package com.group3.vitamins.file.presentation.api.response;

import com.group3.vitamins.file.application.result.BlockFileListResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Schema(description = "블록 파일 목록 응답(§3). 문서별 최신 완료 버전 정보. deleted=true 면 휴지통.")
public record BlockFileListResponse(
        Long blockId,
        @Schema(description = "요청자가 편집 가능한지(업로드·삭제 버튼 노출용)") boolean canEdit,
        List<Item> content
) {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public record Item(
            Long fileId,
            String name,
            Long latestVersionId,
            int latestVersionNo,
            int versionCount,
            String originalFileName,
            String extension,
            long sizeBytes,
            boolean previewable,
            String uploaderName,
            String uploaderDepartment,
            String uploaderPosition,
            String updatedAt,
            @Schema(description = "휴지통 진입 시각. deleted=false 면 항상 null") String deletedAt,
            @Schema(description = "낙관락 버전. 문서명 수정 시 이 값을 그대로 보낸다.", example = "3") int version
    ) {
    }

    public static BlockFileListResponse from(BlockFileListResult r) {
        List<Item> items = r.content().stream()
                .map(i -> new Item(
                        i.fileId(), i.name(), i.latestVersionId(), i.latestVersionNo(), i.versionCount(),
                        i.originalFileName(), i.extension(), i.sizeBytes(), i.previewable(),
                        i.uploaderName(), i.uploaderDepartment(), i.uploaderPosition(),
                        i.updatedAt() == null ? null : i.updatedAt().format(FMT),
                        i.deletedAt() == null ? null : i.deletedAt().format(FMT),
                        i.version()))
                .toList();
        return new BlockFileListResponse(r.blockId(), r.canEdit(), items);
    }
}
