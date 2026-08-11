package com.group3.vitamins.file.presentation.api.response;

import com.group3.vitamins.file.application.result.VersionHistoryResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Schema(description = "버전 이력 조회 응답(§8). 차수 내림차순.")
public record VersionHistoryResponse(
        Long fileId,
        String name,
        int versionCount,
        List<Item> content
) {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Schema(name = "VersionHistoryResponseItem")
    public record Item(
            Long fileVersionId,
            int versionNo,
            boolean latest,
            String originalFileName,
            String extension,
            long sizeBytes,
            Integer pageCount,
            boolean previewable,
            String comment,
            String uploaderName,
            String uploaderDepartment,
            String uploaderPosition,
            String completedAt
    ) {
    }

    public static VersionHistoryResponse from(VersionHistoryResult r) {
        List<Item> items = r.content().stream()
                .map(i -> new Item(
                        i.fileVersionId(), i.versionNo(), i.latest(), i.originalFileName(), i.extension(),
                        i.sizeBytes(), i.pageCount(), i.previewable(), i.comment(),
                        i.uploaderName(), i.uploaderDepartment(), i.uploaderPosition(),
                        i.completedAt() == null ? null : i.completedAt().format(FMT)))
                .toList();
        return new VersionHistoryResponse(r.fileId(), r.name(), r.versionCount(), items);
    }
}
