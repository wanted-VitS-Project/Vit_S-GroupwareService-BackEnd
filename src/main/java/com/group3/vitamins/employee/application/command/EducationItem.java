package com.group3.vitamins.employee.application.command;

/**
 * 등록·수정 요청의 학력 한 건 (`employee.md` §3·§4). 컨트롤러가 받은 <b>가공 전</b> 값을 담는다 —
 * {@code degree} 문자열→enum 변환, 마스터 존재검사, 길이 검증은 {@code EmployeeCommandService} 가 한다.
 *
 * @param majorId 전공 마스터 ID (필수 — 없으면 EMP_INVALID_REQUEST)
 * @param degree  학위 코드 {@code BACHELOR}·{@code MASTER}·{@code DOCTOR} (필수)
 * @param school  학교 (선택)
 */
public record EducationItem(
        Long majorId,
        String degree,
        String school
) {
}
