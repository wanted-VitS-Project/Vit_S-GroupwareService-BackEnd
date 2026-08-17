package com.group3.vitamins.employee.application.result;

/**
 * 사원 이름 검색 결과 한 행 (MyBatis 조회 · `.ai/api/employee.md` §9).
 *
 * <p>결재자 후보 표시에 필요한 최소 필드만 담는다 — <b>급여 등 민감 정보는 절대 포함하지 않는다.</b>
 * {@code department}·{@code position} 은 동명이인 구분용이며 미배정이면 null.
 * {@code profileImageKey} 는 아바타 노출 여부 판정용(있으면 응답이 서빙 URL, 없으면 null) — 키 자체는 응답에 내보내지 않는다.
 */
public record EmployeeSearchRow(
        String userId,
        String name,
        String department,
        String position,
        String profileImageKey
) {
}
