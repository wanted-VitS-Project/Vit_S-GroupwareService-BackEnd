package com.group3.vitamins.project.block.application.result;

import java.time.LocalDateTime;

/** 제목·담당자 수정 결과. 해제했으면 title·owner 가 null 이다. */
public record BlockUpdateResult(
        Long blockId,
        String title,
        BlockOwner owner,
        LocalDateTime updatedAt
) {
}