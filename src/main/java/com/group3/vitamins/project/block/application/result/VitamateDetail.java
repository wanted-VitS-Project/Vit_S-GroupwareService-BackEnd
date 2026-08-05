package com.group3.vitamins.project.block.application.result;

// AI 블록 상세. 분석 이력과 결과는 비타메이트 전용 API에서 별도로 조회한다.
public record VitamateDetail(
        Long vitamateBlockId,
        String welcomeMessage
) implements BlockDetail {
}
