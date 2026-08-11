package com.group3.vitamins.project.step.application.command;

import java.util.List;

/**
 * 스텝 위치 일괄 재정렬. 보드 전체를 받는다는 계약이라 목록에 없는 스텝은 손대지 않는다.
 * stageId 는 이미 정규화된 값이다 — null 이면 미소속이고, 명세의 {@code 0} 은 여기 오지 않는다.
 */
public record ReorderStepsCommand(
        Long projectId,
        List<Item> items,
        String requesterUserId,
        String role
) {

    /**
     * @param version 이 스텝을 조회했을 때의 버전. <b>항목마다 따로 검사한다</b> —
     *                하나라도 어긋나면 요청 전체가 롤백된다 (`CONCURRENCY.md` §4-2)
     */
    public record Item(Long stepId, Long stageId, int sortOrder, int version) {
    }
}
