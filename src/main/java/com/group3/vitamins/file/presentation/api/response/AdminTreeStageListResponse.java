package com.group3.vitamins.file.presentation.api.response;

import com.group3.vitamins.file.application.result.AdminTreeStageProjection;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** 전사 파일 트리 — 스테이지 목록(§14.2). 미분류 버킷은 맨 뒤에 stageId=null 로 온다. */
@Schema(description = "전사 파일 트리 스테이지 목록(§14.2)")
public record AdminTreeStageListResponse(
        @Schema(description = "스테이지 목록. 미분류 버킷은 맨 뒤 stageId=null") List<Stage> stages
) {

    @Schema(description = "스테이지 노드")
    public record Stage(
            @Schema(description = "스테이지 번호. 미분류 버킷은 null", nullable = true) Long stageId,
            @Schema(description = "스테이지명. 미분류 버킷은 '미분류'") String name,
            @Schema(description = "정렬 순서") int sortOrder
    ) {
        static Stage from(AdminTreeStageProjection p) {
            return new Stage(p.stageId(), p.name(), p.sortOrder());
        }
    }

    public static AdminTreeStageListResponse from(List<AdminTreeStageProjection> list) {
        return new AdminTreeStageListResponse(list.stream().map(Stage::from).toList());
    }
}
