package com.group3.vitamins.employee.application.result;

/**
 * 엑셀 일괄 등록 <b>행별 검증 결과 코드</b> (employee.md §7 — 계약 고정).
 * ⚠️ 명세에 있는 값만 쓴다. 임의로 추가하지 말 것 — 프론트가 이 값으로 오류를 분기한다.
 * (2026-08-13 학력/자격증 도입으로 명세에 {@code EDU_NOT_FOUND}·{@code CERT_NOT_FOUND} 추가됨.)
 *
 * <p>{@code REQUIRED_COLUMN} 은 "필수 컬럼 누락"뿐 아니라 <b>그 값이 형식상 쓸 수 없는 경우</b>(입사일 형식 오류·
 * 권한 값 오류·이메일 형식 오류·학위 표기 오류)까지 담는 버킷으로 쓴다 — 명세 enum 에 형식 오류용 코드가 따로 없기 때문이다(메시지로 구분).
 */
public enum BulkValidation {
    REQUIRED_COLUMN,
    USER_ID_DUPLICATED,
    DEPARTMENT_NOT_FOUND,
    ADMIN_ROLE_NOT_ALLOWED,
    EDU_NOT_FOUND,
    CERT_NOT_FOUND
}
