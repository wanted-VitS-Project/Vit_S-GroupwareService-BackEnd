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
     * 4필드는 필수지만 래퍼 타입이다 — int 면 JSON 누락 시 Jackson 이 0 을 채워
     * 블록이 조용히 0행 0열로 옮겨진다. 누락은 400 으로 잡아야 한다.
     *
     * @param version 이 블록을 조회했을 때의 버전. <b>항목마다 따로 검사한다</b> —
     *                하나라도 어긋나면 요청 전체가 롤백된다 (`CONCURRENCY.md` §4-2)
     */
    public record BlockLayout(Long blockId, Integer rowIndex, Integer sortOrder,
                              Integer colSpan, Integer version) {
    }
}
