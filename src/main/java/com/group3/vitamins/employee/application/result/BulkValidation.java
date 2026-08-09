package com.group3.vitamins.employee.application.result;

/**
 * 엑셀 일괄 등록 <b>행별 검증 결과 코드</b> (employee.md §7 — 계약 고정 4종).
 * ⚠️ 명세에 이 4개만 있다. 새 값을 추가하지 말 것 — 프론트가 이 값으로 오류를 분기한다.
 *
 * <p>{@code REQUIRED_COLUMN} 은 "필수 컬럼 누락"뿐 아니라 <b>그 값이 형식상 쓸 수 없는 경우</b>(입사일 형식 오류·
 * 권한 값 오류·이메일 형식 오류)까지 담는 버킷으로 쓴다 — 명세 enum 에 형식 오류용 코드가 따로 없기 때문이다(메시지로 구분).
 */
public enum BulkValidation {
    REQUIRED_COLUMN,
    USER_ID_DUPLICATED,
    DEPARTMENT_NOT_FOUND,
    ADMIN_ROLE_NOT_ALLOWED
}
