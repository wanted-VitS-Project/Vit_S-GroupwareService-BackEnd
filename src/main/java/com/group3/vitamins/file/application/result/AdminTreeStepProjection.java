package com.group3.vitamins.file.application.result;

/** 전사 파일 트리(§14.3) 스텝 노드 projection. */
public record AdminTreeStepProjection(
        Long stepId,
        String name,
        int sortOrder,
        String status
) {
}
