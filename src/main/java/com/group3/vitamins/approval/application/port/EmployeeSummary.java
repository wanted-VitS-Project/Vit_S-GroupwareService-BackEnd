package com.group3.vitamins.approval.application.port;

/** {@code employee} 라이브 조회 결과 중 결재 도메인이 필요로 하는 최소 정보 (INV-11) */
public record EmployeeSummary(String userId, String name, String position, String department, String role) {
}
