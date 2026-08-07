package com.group3.vitamins.vitamate.analysis.infrastructure.persistence.row;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// MyBatis가 조회한 비타메이트 분석 요약 행
@Getter
@Setter
public class VitamateAnalysisRow {

    private Long analysisId;
    private Long blockId;
    private String reviewType;
    private String reviewCategoryCodes;
    private String additionalInstruction;
    private String analysisStatus;
    private String result;
    private String errorMessage;
    private String requestHash;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
