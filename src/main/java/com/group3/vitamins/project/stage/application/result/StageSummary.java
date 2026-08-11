package com.group3.vitamins.project.stage.application.result;

/**
 * @param version 🚨 <b>빠뜨리면 안 된다.</b> 프론트가 수정 요청에 실어 보낼 값이 여기서만 나온다.
 *                없으면 프론트가 0/null 을 보내 <b>모든 저장이 409</b> 가 되는데,
 *                컴파일도 테스트도 통과한다 (`CONCURRENCY.md` §6-3)
 */
public record StageSummary(
        Long stageId,
        String name,
        int sortOrder,
        int stepCount,
        int version
) {
}
