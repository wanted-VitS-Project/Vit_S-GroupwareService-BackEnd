package com.group3.vitamins.vitamate.analysis.infrastructure.persistence.row;

import lombok.Getter;
import lombok.Setter;

// MyBatis가 조회한 비타메이트 검토 유형 마스터 행입니다.
@Getter
@Setter
public class VitamateReviewTemplateGroupRow {

    private String reviewType;
    private String reviewTypeName;
    private String description;
}
