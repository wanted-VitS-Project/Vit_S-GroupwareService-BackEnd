package com.group3.vitamins.project.block.application.result;

/** 반영된 배치 한 건. 요청 순서 그대로 돌려준다. */
public record BlockLayoutResult(
        Long blockId,
        int rowIndex,
        int sortOrder,
        int colSpan
) {
}