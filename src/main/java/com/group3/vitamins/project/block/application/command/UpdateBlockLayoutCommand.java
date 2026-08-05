package com.group3.vitamins.project.block.application.command;

import java.util.List;

/** 드래그 결과 일괄 반영 (BLK-003·BLK-004). */
public record UpdateBlockLayoutCommand(
        Long stepId,
        List<BlockLayout> layouts,
        String requesterUserId,
        String role
) {

    /**
     * 3필드는 필수지만 래퍼 타입이다 — int 면 JSON 누락 시 Jackson 이 0 을 채워
     * 블록이 조용히 0행 0열로 옮겨진다. 누락은 400 으로 잡아야 한다.
     */
    public record BlockLayout(Long blockId, Integer rowIndex, Integer sortOrder, Integer colSpan) {
    }
}