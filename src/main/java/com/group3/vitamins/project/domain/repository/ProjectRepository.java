package com.group3.vitamins.project.domain.repository;

import com.group3.vitamins.project.domain.model.Project;

import java.util.Optional;

public interface ProjectRepository {
    Project save(Project project);

    /** 같은 회사에서 같은 공고로 이미 만들어진 프로젝트가 있는지 확인한다 (`uk_project_bid_notice_company`). */
    Optional<Project> findByBidNoticeId(Long bidNoticeId, Long companyId);

    /** 논리 삭제되지 않은 프로젝트를 회사 범위로 조회한다. 타사 프로젝트는 조회되지 않아 404 로 귀결된다. */
    Optional<Project> findById(Long projectId, Long companyId);
}