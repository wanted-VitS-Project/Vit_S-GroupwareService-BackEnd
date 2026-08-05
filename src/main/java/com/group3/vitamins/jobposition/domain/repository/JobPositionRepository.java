package com.group3.vitamins.jobposition.domain.repository;

import com.group3.vitamins.jobposition.domain.model.JobPosition;

import java.util.List;
import java.util.Optional;

/**
 * 직급 영속성 아웃바운드 포트. 구현체는 {@code infrastructure/persistence} 의 JPA 어댑터다.
 *
 * <p>⚠️ {@code employee} 를 가로지르는 사용 인원 집계는 이 포트가 아니라
 * {@code application/port/JobPositionEmployeeCountPort}(MyBatis) 가 담당한다 — 남의 테이블이기 때문이다.
 */
public interface JobPositionRepository {

    /** 정렬 순서(오름차순), 같으면 직급명(오름차순) 목록. 페이징 없이 전건을 내린다 (`job-position.md` §1). */
    List<JobPosition> findAllOrdered();

    /** 직급명으로 조회한다 (중복 검사용 — DB UNIQUE 와 이중 방어). */
    Optional<JobPosition> findByName(String name);

    /** ID 로 조회한다 (수정·삭제의 404 판정용). */
    Optional<JobPosition> findById(Long jobPositionId);

    /**
     * 다음 정렬 순서 = 현재 최대값 + 1. 비어 있으면 1.
     * 생성 시 {@code sortOrder} 를 생략하면 마지막 순서 뒤에 붙인다 (`job-position.md` §2).
     */
    int nextSortOrder();

    /** 새로 만들거나 변경된 직급을 저장한다. */
    JobPosition save(JobPosition jobPosition);

    /** 직급을 물리 삭제한다 (소프트 삭제 아님 — `job-position.md` §4). */
    void delete(JobPosition jobPosition);
}
