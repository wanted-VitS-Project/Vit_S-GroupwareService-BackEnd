package com.group3.vitamins.project.domain.repository;

import java.util.List;

/** {@code project_business_category} 조인 테이블 — 프로젝트 도메인 소관 (BCT 쪽 포트 javadoc 참고). */
public interface ProjectBusinessCategoryRepository {
    void linkAll(Long projectId, List<Long> businessCategoryIds);

    /** 프로젝트에 연결된 카테고리 ID 를 조회한다. */
    List<Long> findCategoryIds(Long projectId);

    /** 연결 행을 지운다. 지울 행이 없었으면 false. 연결 행이라 하드 삭제다. */
    boolean unlink(Long projectId, Long businessCategoryId);
}