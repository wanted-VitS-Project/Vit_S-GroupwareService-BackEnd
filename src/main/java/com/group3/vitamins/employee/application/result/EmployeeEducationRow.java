package com.group3.vitamins.employee.application.result;

/**
 * 사원 상세의 학력 한 행 (`employee.md` §2 {@code data.educations[]}). 전공은 마스터를 조인해
 * 이름을 함께 담는다(스냅샷 아님) — 마스터명이 바뀌면 상세도 최신 이름을 보인다.
 *
 * @param majorId   전공 마스터 ID
 * @param majorName 전공명 (마스터 조인)
 * @param degree    학위 enum 이름 {@code BACHELOR}·{@code MASTER}·{@code DOCTOR}
 * @param school    학교 (null 허용)
 */
public record EmployeeEducationRow(
        Long majorId,
        String majorName,
        String degree,
        String school
) {
}
