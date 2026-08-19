package com.group3.vitamins.employee.application.result;

/**
 * 검증을 통과한 행의 자격증 한 건 — 등록 직전 상태 (employee.md §8).
 * {@code certificateId} 가 {@code null} 이면 마스터에 아직 없는 자격증({@code autoCreateMasters=true} 에서만 가능).
 */
public record RowCertificate(String certificateName, Long certificateId) {
}
