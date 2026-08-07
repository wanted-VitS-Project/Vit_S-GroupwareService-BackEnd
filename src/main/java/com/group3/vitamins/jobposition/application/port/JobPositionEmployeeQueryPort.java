package com.group3.vitamins.jobposition.application.port;

import com.group3.vitamins.jobposition.application.result.JobPositionEmployeeRow;

import java.util.List;

/**
 * 직급에 속한 <b>사원 목록</b> 조회 아웃바운드 포트 (`.ai/api/job-position.md` §5). 사원은 사원 도메인 소관
 * 테이블({@code employee})이라 도메인 리포지토리(JPA)가 아니라 이 포트로 분리하고, 실제 조회는
 * {@code infrastructure/adapter} 의 MyBatis 어댑터가 처리한다 ({@link JobPositionEmployeeCountPort} 선례).
 *
 * <p>모집단 기준은 §1 {@code employeeCount} 와 동일하다 — 시스템 계정·퇴사자·삭제 사원 제외
 * ({@code is_system = 0 AND resigned_at IS NULL AND deleted_at IS NULL}). 그래서 목록 길이 = 그 직급의
 * {@code employeeCount} 가 된다.
 */
public interface JobPositionEmployeeQueryPort {

    /**
     * 해당 직급에 속한 사원을 이름 오름차순(동명이인은 사번 오름차순)으로 조회한다.
     * 없으면 빈 목록. 직급 존재 여부는 이 포트가 아니라 서비스가 판정한다({@code POS_NOT_FOUND}).
     */
    List<JobPositionEmployeeRow> findEmployeesByJobPosition(Long jobPositionId);
}
