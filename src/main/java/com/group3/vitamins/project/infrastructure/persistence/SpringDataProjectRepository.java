package com.group3.vitamins.project.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataProjectRepository extends JpaRepository<ProjectJpaEntity, Long> {

    /** 같은 공고로 이미 만들어진 프로젝트가 있는지 확인한다 (`UNIQUE(bid_notice_id)`). */
    /**
     * 논리 삭제분은 제외한다. 삭제 시 bid_notice_id 를 비우므로 삭제분이 걸릴 일은 없지만,
     * 과거 데이터가 남아 있을 수 있어 조회에서도 한 번 더 막는다.
     */
    Optional<ProjectJpaEntity> findByBidNoticeIdAndDeletedAtIsNull(Long bidNoticeId);

    /** 논리 삭제분은 조회하지 않는다. */
    Optional<ProjectJpaEntity> findByProjectIdAndDeletedAtIsNull(Long projectId);
}