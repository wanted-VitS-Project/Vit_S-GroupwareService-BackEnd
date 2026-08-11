package com.group3.vitamins.project.stage.application.command;

import java.util.List;

/** 스테이지 순서 일괄 재정렬. 사이드바 전체의 최종 순서를 받는다 (STG-002). */
public record ReorderStagesCommand(
        Long projectId,
        List<Item> items,
        String requesterUserId,
        String role
) {

    /**
     * @param version 이 스테이지를 조회했을 때의 버전. <b>항목마다 따로 검사한다</b> —
     *                하나라도 어긋나면 요청 전체가 롤백된다 (`CONCURRENCY.md` §4-2)
     */
    public record Item(Long stageId, int sortOrder, int version) {
    }
}
