package com.group3.vitamins.employee.application.support;

import java.util.List;

/**
 * 엑셀 일괄 등록 템플릿의 <b>컬럼 순서</b> 단일 정의 (employee.md §6). 템플릿 생성(헤더 출력)과 업로드 파싱(열→필드 매핑)이
 * 이 상수를 공유해 두 경로가 어긋나지 않게 한다. 순서를 바꾸면 파싱 인덱스도 함께 바뀐다.
 */
public final class EmployeeBulkColumns {

    public static final int USER_ID = 0;
    public static final int NAME = 1;
    public static final int DEPARTMENT = 2;
    public static final int JOB_POSITION = 3;
    public static final int HIRED_AT = 4;
    public static final int EMAIL = 5;
    public static final int PHONE = 6;
    public static final int ROLE = 7;
    public static final int EDUCATION = 8;   // 학력 — "전공:학위" 여러 개는 ; · , · 줄바꿈 구분 (선택)
    public static final int CERTIFICATE = 9; // 자격증 — 자격증명 여러 개는 ; · , · 줄바꿈 구분 (선택)

    /** 헤더 라벨 — 인덱스 순서가 위 상수와 일치한다. */
    public static final List<String> HEADERS = List.of(
            "사번", "이름", "부서명", "직급명", "입사일", "이메일", "연락처", "권한", "학력", "자격증");

    /** 데이터가 시작되는 행(0-base) — 0행은 헤더. */
    public static final int FIRST_DATA_ROW = 1;

    private EmployeeBulkColumns() {
    }
}
