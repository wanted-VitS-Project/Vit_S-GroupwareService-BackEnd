package com.group3.vitamins.project.block.application.result;

/** 체크리스트 항목 하나. chk_id 오름차순으로 담긴다 (sort_order 컬럼이 없다). */
public record ChecklistItemView(Long chkId, String content, boolean isCompleted) {
}