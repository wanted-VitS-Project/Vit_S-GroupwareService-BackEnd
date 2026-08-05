package com.group3.vitamins.vitamate.infrastructure.persistence.row;

import lombok.Getter;
import lombok.Setter;

// MyBatis가 조회한 비타메이트 분석 근거 행
@Getter
@Setter
public class VitamateAnalysisCitationRow {

    private Integer rankOrder;
    private Long fileVersionId;
    private Long documentChunkId;
    private Integer pageNumber;
    private String excerpt;
}
