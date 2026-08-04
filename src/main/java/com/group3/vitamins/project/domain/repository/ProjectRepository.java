package com.group3.vitamins.project.domain.repository;

import com.group3.vitamins.project.domain.model.Project;

import java.util.Optional;

public interface ProjectRepository {
    Project save(Project project);

    /** 같은 공고로 이미 만들어진 프로젝트가 있는지 확인한다 (`UNIQUE(bid_notice_id)`). */
    Optional<Project> findByBidNoticeId(Long bidNoticeId);
}