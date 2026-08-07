package com.group3.vitamins.vitamate.analysis.infrastructure.persistence.row;

import lombok.Getter;
import lombok.Setter;

// MyBatis가 조회한 비타메이트 분석 대상 문서 행
@Getter
@Setter
public class VitamateAnalysisDocumentRow {

    private Long fileVersionId;
    private String fileName;
    private String documentRole;
}
