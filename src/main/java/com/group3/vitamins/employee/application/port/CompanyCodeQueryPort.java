package com.group3.vitamins.employee.application.port;

/**
 * 사원 등록 시 사번 앞에 붙일 <b>현재 회사의 코드</b>를 조회하는 아웃바운드 포트.
 *
 * <p>세션에는 {@code company_id}(숫자)만 실리고 회사코드는 파싱 대상이 아니므로, 접두사 재료인 코드는
 * 여기서 {@code company} 테이블을 조회해 얻는다. 소비자(employee)가 소유하며 MyBatis 어댑터가 구현한다
 * (아키텍처 §2-1, {@link EmployeeReferenceQueryPort} 와 동일한 결).
 */
public interface CompanyCodeQueryPort {

    /** {@code company_id} 로 회사코드를 조회한다. 없으면 {@code null}. */
    String findCodeByCompanyId(Long companyId);
}
