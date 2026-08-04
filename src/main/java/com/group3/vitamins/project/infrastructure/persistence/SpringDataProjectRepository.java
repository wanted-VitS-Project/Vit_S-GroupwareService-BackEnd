package com.group3.vitamins.project.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataProjectRepository extends JpaRepository<ProjectJpaEntity, Long> {

    /** 같은 공고로 이미 만들어진 프로젝트가 있는지 확인한다 (`UNIQUE(bid_notice_id)`). */
    Optional<ProjectJpaEntity> findByBidNoticeId(Long bidNoticeId);

    /** 논리 삭제분은 조회하지 않는다. */
    Optional<ProjectJpaEntity> findByProjectIdAndDeletedAtIsNull(Long projectId);
}