package com.group3.vitamins.project.block.infrastructure.adapter;

/** checklist 테이블 조회 행. 블록 하나에 여러 행이 온다. */
public record ChecklistItemRow(Long chkBlockId, Long chkId, String content, boolean isCompleted) {
}