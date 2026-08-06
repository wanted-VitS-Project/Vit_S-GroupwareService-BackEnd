package com.group3.vitamins.vitamate.analysis.infrastructure.persistence.row;

import lombok.Getter;
import lombok.Setter;

// MyBatis가 조회한 Python 분석 작업 기본 정보 행
@Getter
@Setter
public class VitamateAnalysisJobRow {

    private Long analysisId;
    private String attemptId;
    private String prompt;
    private Long projectId;
    private Long blockId;
}
