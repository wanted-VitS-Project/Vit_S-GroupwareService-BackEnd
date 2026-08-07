package com.group3.vitamins.vitamate.analysis.infrastructure.persistence.row;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// MyBatis가 조회한 비타메이트 분석 실행 이력 한 건을 담는 Row입니다.
@Getter
@Setter
public class VitamateAnalysisHistoryRow {

    private Long analysisId;
    private String prompt;
    private String analysisStatus;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}