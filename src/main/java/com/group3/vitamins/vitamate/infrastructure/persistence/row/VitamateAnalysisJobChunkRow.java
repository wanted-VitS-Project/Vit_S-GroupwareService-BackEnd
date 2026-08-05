package com.group3.vitamins.vitamate.infrastructure.persistence.row;

import lombok.Getter;
import lombok.Setter;

// MyBatis가 조회한 Python 분석 작업 후보 청크 행
@Getter
@Setter
public class VitamateAnalysisJobChunkRow {

    private Long fileVersionId;
    private Long documentChunkId;
    private String chromaId;
    private Integer pageNumber;
    private String excerpt;
}
