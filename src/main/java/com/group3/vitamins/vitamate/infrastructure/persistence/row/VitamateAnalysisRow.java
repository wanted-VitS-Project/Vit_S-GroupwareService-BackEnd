package com.group3.vitamins.vitamate.infrastructure.persistence.row;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// MyBatis가 조회한 비타메이트 분석 요약 행
@Getter
@Setter
public class VitamateAnalysisRow {

    private Long analysisId;
    private String prompt;
    private String analysisStatus;
    private String requestHash;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
