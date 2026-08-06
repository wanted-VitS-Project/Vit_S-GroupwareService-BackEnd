package com.group3.vitamins.vitamate.analysis.infrastructure.persistence.row;

import lombok.Getter;
import lombok.Setter;

// MyBatis가 조회한 비타메이트 블록 접근 컨텍스트 행
@Getter
@Setter
public class VitamateBlockContextRow {

    private Long blockId;
    private Long vitamateBlockId;
    private Long stepId;
    private Long projectId;
}
