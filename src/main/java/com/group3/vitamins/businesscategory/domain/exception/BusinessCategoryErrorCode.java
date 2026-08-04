package com.group3.vitamins.businesscategory.domain.exception;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BusinessCategoryErrorCode implements ErrorCode {

    BUSINESS_CATEGORY_NAME_REQUIRED("BUSINESS_CATEGORY_NAME_REQUIRED",
            "카테고리 이름을 입력해 주세요."),
    BUSINESS_CATEGORY_FIELD_TOO_LONG("BUSINESS_CATEGORY_FIELD_TOO_LONG",
            "이름은 100자, 업무코드는 30자를 넘을 수 없습니다."),
    BUSINESS_CATEGORY_NO_FIELD_TO_UPDATE("BUSINESS_CATEGORY_NO_FIELD_TO_UPDATE",
            "수정할 항목이 없습니다."),

    BUSINESS_CATEGORY_ADMIN_ONLY("BUSINESS_CATEGORY_ADMIN_ONLY",
            "관리자만 사업 카테고리를 관리할 수 있습니다."),

    BUSINESS_CATEGORY_NOT_FOUND("BUSINESS_CATEGORY_NOT_FOUND",
            "사업 카테고리를 찾을 수 없습니다."),

    BUSINESS_CATEGORY_NAME_DUPLICATED("BUSINESS_CATEGORY_NAME_DUPLICATED",
            "같은 이름의 카테고리가 이미 있습니다."),
    BUSINESS_CATEGORY_CODE_DUPLICATED("BUSINESS_CATEGORY_CODE_DUPLICATED",
            "같은 업무코드의 카테고리가 이미 있습니다."),
    BUSINESS_CATEGORY_IN_USE("BUSINESS_CATEGORY_IN_USE",
            "사용 중인 카테고리는 삭제할 수 없습니다.");

    private final String code;
    private final String message;
}