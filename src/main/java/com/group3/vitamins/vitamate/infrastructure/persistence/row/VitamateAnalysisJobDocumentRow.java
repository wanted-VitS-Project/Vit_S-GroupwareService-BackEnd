package com.group3.vitamins.vitamate.infrastructure.persistence.row;

import lombok.Getter;
import lombok.Setter;

// MyBatis가 조회한 Python 분석 작업 대상 문서 행
@Getter
@Setter
public class VitamateAnalysisJobDocumentRow {

    private Long fileVersionId;
    private String fileName;
}
