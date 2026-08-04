package com.group3.vitamins.approval.application.port;

import java.util.Optional;

/**
 * 계정/사원 도메인(김동현님 소관)에 물어보는 아웃바운드 포트 — INV-11(결재선 부서·직책은 스냅샷 없이
 * 항상 {@code employee} 라이브 조회).
 *
 * <p>Block/Project 포트와 달리 <b>스텁이 아니다</b> — Account/Auth 도메인은 이미 구현돼 있어
 * {@code AuthQueryMapper} 를 그대로 재사용한다.
 */
public interface EmployeeCatalogPort {

    Optional<EmployeeSummary> findEmployee(String userId);
}
