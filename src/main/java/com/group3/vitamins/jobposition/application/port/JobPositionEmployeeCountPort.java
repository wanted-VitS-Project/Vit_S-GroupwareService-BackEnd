package com.group3.vitamins.jobposition.application.port;

import java.util.Map;

/**
 * 직급별 <b>사용 인원</b> 집계를 위한 아웃바운드 포트. 집계는 사원 도메인 소관 테이블({@code employee})에
 * 걸리므로 도메인 리포지토리(JPA)가 아니라 이 포트로 분리하고, 실제 조회는
 * {@code infrastructure/adapter} 의 MyBatis 어댑터가 처리한다 (department 의 {@code DepartmentEmployeeQueryPort} 선례).
 *
 * <p>집계 기준은 <b>시스템 계정·퇴사자·삭제 사원 제외</b> ({@code is_system = 0 AND resigned_at IS NULL
 * AND deleted_at IS NULL}) — 부서 인원 집계와 동일하다 (`.ai/api/job-position.md` POS-002).
 */
public interface JobPositionEmployeeCountPort {

    /**
     * 직급별 사용 인원. 목록 항목마다 따로 세면 N+1 이 되므로 한 번에 받아 대조한다.
     * 인원이 0 인 직급은 결과에 없다 — 서비스에서 0 으로 채운다.
     *
     * @return {@code jobPositionId → employeeCount}
     */
    Map<Long, Integer> countByJobPosition();

    /** 직급 1건의 사용 인원 — 삭제 차단({@code POS_IN_USE}) 판정용. 목록과 같은 제외 기준. */
    long countByJobPositionId(Long jobPositionId);
}
