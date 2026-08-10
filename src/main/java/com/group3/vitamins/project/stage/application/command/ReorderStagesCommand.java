package com.group3.vitamins.project.stage.application.command;

import java.util.List;

/** 스테이지 순서 일괄 재정렬. 사이드바 전체의 최종 순서를 받는다 (STG-002). */
public record ReorderStagesCommand(
        Long projectId,
        List<Item> items,
        String requesterUserId,
        String role
) {

    public record Item(Long stageId, int sortOrder) {
    }
}
