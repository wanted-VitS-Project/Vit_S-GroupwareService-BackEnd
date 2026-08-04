package com.group3.vitamins.project.domain.repository;

import java.util.List;

/** {@code project_business_category} 조인 테이블 — 프로젝트 도메인 소관 (BCT 쪽 포트 javadoc 참고). */
public interface ProjectBusinessCategoryRepository {
    void linkAll(Long projectId, List<Long> businessCategoryIds);
}