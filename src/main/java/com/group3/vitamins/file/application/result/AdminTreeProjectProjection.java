package com.group3.vitamins.file.application.result;

import java.time.LocalDateTime;

/** 전사 파일 트리(§14.1) 프로젝트 노드 projection. */
public record AdminTreeProjectProjection(
        Long projectId,
        String name,
        String status,
        String clientName,
        LocalDateTime updatedAt
) {
}
