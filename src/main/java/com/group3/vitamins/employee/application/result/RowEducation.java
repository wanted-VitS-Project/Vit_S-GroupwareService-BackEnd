package com.group3.vitamins.employee.application.result;

import com.group3.vitamins.employee.domain.model.Degree;

/**
 * 검증을 통과한 행의 학력 한 건 — 등록 직전 상태 (employee.md §8).
 * {@code majorId} 가 {@code null} 이면 <b>마스터에 아직 없는 전공</b>이다({@code autoCreateMasters=true} 에서만 가능) —
 * 등록 단계가 마스터를 먼저 만든 뒤 이름으로 ID 를 채워 {@code EmployeeEducation} 으로 굳힌다.
 */
public record RowEducation(String majorName, Long majorId, Degree degree) {
}
