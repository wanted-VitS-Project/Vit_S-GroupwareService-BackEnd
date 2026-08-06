package com.group3.vitamins.vitamate.analysis.application.result;

import com.group3.vitamins.vitamate.analysis.application.port.VitamateAnalysisReaderPort;

import java.time.LocalDateTime;
import java.util.List;

// 비타메이트 블록별 분석 실행 이력 조회 결과입니다.
public record VitamateAnalysisHistoryResult(
        Long blockId,
        List<Item> content
) {

    // Reader Port에서 조회한 이력 목록을 application result로 변환합니다.
    public static VitamateAnalysisHistoryResult from(
            Long blockId,
            List<VitamateAnalysisReaderPort.VitamateAnalysisHistory> histories
    ) {
        return new VitamateAnalysisHistoryResult(
                blockId,
                histories.stream()
                        .map(Item::from)
                        .toList()
        );
    }

    public record Item(
            Long analysisId,
            String prompt,
            String analysisStatus,
            LocalDateTime createdAt,
            LocalDateTime completedAt
    ) {

        // Reader Port의 이력 값을 응답에 가까운 item 값으로 변환합니다.
        private static Item from(VitamateAnalysisReaderPort.VitamateAnalysisHistory history) {
            return new Item(
                    history.analysisId(),
                    history.prompt(),
                    history.analysisStatus(),
                    history.createdAt(),
                    history.completedAt()
            );
        }
    }
}