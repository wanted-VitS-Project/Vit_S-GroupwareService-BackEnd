package com.group3.vitamins.file.presentation.api.response;

import com.group3.vitamins.file.application.result.AdminTreeProjectPageResult;
import com.group3.vitamins.file.application.result.AdminTreeProjectProjection;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.format.DateTimeFormatter;
import java.util.List;

/** 전사 파일 트리 — 프로젝트 페이지(§14.1). */
@Schema(description = "전사 파일 트리 프로젝트 페이지(§14.1)")
public record AdminTreeProjectPageResponse(
        @Schema(description = "현재 페이지 항목") List<Project> content,
        @Schema(description = "0-base 페이지") int page,
        @Schema(description = "페이지 크기") int size,
        @Schema(description = "전체 건수") long totalElements,
        @Schema(description = "전체 페이지 수") int totalPages
) {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Schema(description = "프로젝트 노드")
    public record Project(
            @Schema(description = "프로젝트 번호") Long projectId,
            @Schema(description = "프로젝트명") String name,
            @Schema(description = "프로젝트 상태") String status,
            @Schema(description = "발주처", nullable = true) String clientName,
            @Schema(description = "최종 수정 시각") String updatedAt
    ) {
        static Project from(AdminTreeProjectProjection p) {
            return new Project(p.projectId(), p.name(), p.status(), p.clientName(),
                    p.updatedAt() == null ? null : p.updatedAt().format(FMT));
        }
    }

    public static AdminTreeProjectPageResponse from(AdminTreeProjectPageResult r) {
        return new AdminTreeProjectPageResponse(
                r.content().stream().map(Project::from).toList(),
                r.page(), r.size(), r.totalElements(), r.totalPages());
    }
}
