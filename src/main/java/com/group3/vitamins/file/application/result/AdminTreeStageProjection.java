package com.group3.vitamins.file.application.result;

/** 전사 파일 트리(§14.2) 스테이지 노드 projection. 미분류 버킷은 서비스가 stageId=null 로 덧붙인다. */
public record AdminTreeStageProjection(
        Long stageId,
        String name,
        int sortOrder
) {
}
