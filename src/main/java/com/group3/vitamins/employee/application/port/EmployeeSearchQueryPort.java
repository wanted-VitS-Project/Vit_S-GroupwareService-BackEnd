package com.group3.vitamins.employee.application.port;

import com.group3.vitamins.employee.application.result.EmployeeSearchRow;

import java.util.List;

/**
 * 사원 이름 검색 아웃바운드 포트. <b>사원·부서·직급 3개 테이블을 가로지르는</b> 조회라
 * JPA 연관관계 대신 MyBatis 로 한 번에 읽는다 (auth 의 {@code UserProfileQueryPort} 선례).
 * 실제 조회는 {@code infrastructure/adapter} 의 MyBatis 어댑터가 처리한다.
 */
public interface EmployeeSearchQueryPort {

    /**
     * 이름 부분 일치로 결재자 후보를 찾는다.
     * <b>시스템 계정·퇴사자·삭제 사원은 제외</b>한다 ({@code is_system=0} · 재직 · 미삭제 — `employee.md` §9).
     *
     * @param name 이름 부분 일치 검색어 (호출 전 null/공백 검증 완료 전제)
     * @param companyId 현재 회사 — 이 회사 사원만 검색한다(타사 사원 노출 차단)
     * @return 후보 목록. 없으면 빈 리스트
     */
    List<EmployeeSearchRow> searchByName(String name, Long companyId);
}
