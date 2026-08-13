package com.group3.vitamins.companydocument.domain.model;

import java.util.Arrays;

/**
 * 사내 문서 분류 (`company_document.category` · COMPANY-DOC-V1 §6-1).
 *
 * <p>고정 enum 이다 — 마스터 CRUD 는 필요해지면 승격한다(§6-1). 한글 표시명은 프론트가 매핑한다.
 * 저장·API 계약은 enum 코드 문자열이다.
 */
public enum DocumentCategory {
    FINANCE,
    COMPANY_INTRO,
    PERFORMANCE,
    CERTIFICATE,
    ETC;

    /** 문자열이 유효한 카테고리인지. 서비스가 형식 검증에 쓴다. */
    public static boolean isValid(String value) {
        return value != null && Arrays.stream(values()).anyMatch(c -> c.name().equals(value));
    }
}
