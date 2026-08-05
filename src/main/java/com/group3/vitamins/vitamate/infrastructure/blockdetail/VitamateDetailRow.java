package com.group3.vitamins.vitamate.infrastructure.blockdetail;

// MyBatis가 조회한 AI 블록 상세 행
public record VitamateDetailRow(
        Long vitamateBlockId,
        String welcomeMessage
) {
}
