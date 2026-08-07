package com.group3.vitamins.vitamate.analysis.infrastructure.persistence.row;

import lombok.Getter;
import lombok.Setter;

// MyBatis가 조회한 비타메이트 검토 카테고리 템플릿 행입니다.
@Getter
@Setter
public class VitamateReviewTemplateRow {

    private String reviewType;
    private String categoryCode;
    private String categoryName;
    private String guideText;
    private String exampleText;
    private String promptTemplate;
    private String templateVersion;
    private Integer sortOrder;
}
