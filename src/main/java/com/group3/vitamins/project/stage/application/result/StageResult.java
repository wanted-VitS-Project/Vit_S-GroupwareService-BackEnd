package com.group3.vitamins.project.stage.application.result;

/**
 * @param version 저장 후의 새 버전. 프론트는 이 값으로 화면 상태를 교체해야
 *                <b>다음 저장이 409 가 되지 않는다</b>
 */
public record StageResult(
        Long stageId,
        Long projectId,
        String name,
        int sortOrder,
        int version
) {
}
