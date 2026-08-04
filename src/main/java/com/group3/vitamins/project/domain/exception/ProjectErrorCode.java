package com.group3.vitamins.project.domain.exception;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProjectErrorCode implements ErrorCode {

    PROJECT_NAME_REQUIRED("PROJECT_NAME_REQUIRED",
            "과업명을 입력해 주세요."),
    PROJECT_NAME_TOO_LONG("PROJECT_NAME_TOO_LONG",
            "과업명은 300자를 넘을 수 없습니다."),
    PROJECT_DATE_RANGE_INVALID("PROJECT_DATE_RANGE_INVALID",
            "시작일은 종료일보다 늦을 수 없습니다."),
    PROJECT_BID_NOTICE_ALREADY_LINKED("PROJECT_BID_NOTICE_ALREADY_LINKED",
            "이미 다른 프로젝트가 연결된 공고입니다.");

    private final String code;
    private final String message;
}