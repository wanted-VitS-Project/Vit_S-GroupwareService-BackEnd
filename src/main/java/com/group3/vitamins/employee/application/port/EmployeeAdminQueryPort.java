package com.group3.vitamins.employee.application.port;

import com.group3.vitamins.employee.application.query.EmployeeListCriteria;
import com.group3.vitamins.employee.application.result.EmployeeDetailRow;
import com.group3.vitamins.employee.application.result.EmployeeGroupRow;
import com.group3.vitamins.employee.application.result.EmployeeListRow;

import java.util.List;
import java.util.Optional;

/**
 * 인사관리용 사원 조회 아웃바운드 포트 (`employee.md` §1·§2). 계정·사원·부서·직급을 가로지르므로 MyBatis 로 구현한다.
 *
 * <p>이름 검색({@code EmployeeSearchQueryPort})과 분리한다 — 그쪽은 로그인 사용자 누구나 쓰는 결재선용
 * 저민감 조회이고, 이쪽은 ADMIN 전용 관리 조회다.
 */
public interface EmployeeAdminQueryPort {

    /** 필터·페이징을 적용한 사원 목록. 시스템 계정은 제외한다. */
    List<EmployeeListRow> findPage(EmployeeListCriteria criteria);

    /** 필터를 적용한 전체 건수 (페이징 제외). 시스템 계정은 제외한다. */
    long count(EmployeeListCriteria criteria);

    /**
     * 사번으로 사원 상세를 조회한다. 시스템 계정도 포함해 반환한다 — 403·404 를 구분하려면 존재 여부와
     * {@code isSystem} 을 서비스가 봐야 하기 때문이다.
     */
    Optional<EmployeeDetailRow> findDetail(String userId, Long companyId);

    /** 사원이 속한 그룹 목록 (그룹명 오름차순). */
    List<EmployeeGroupRow> findGroups(String userId);
}
