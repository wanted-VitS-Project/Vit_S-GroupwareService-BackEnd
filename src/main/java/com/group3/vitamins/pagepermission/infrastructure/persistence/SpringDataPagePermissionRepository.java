package com.group3.vitamins.pagepermission.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 페이지 권한 쓰기·단건 조회 (Spring Data JPA). 명단·집계처럼 employee 를 가로지르는 조회는 MyBatis 가 맡는다.
 */
public interface SpringDataPagePermissionRepository extends JpaRepository<PagePermissionJpaEntity, Long> {

    /** 사용자에게 부여된 행 전부(BIDDING·FINANCE) — my/pages 판정용. */
    List<PagePermissionJpaEntity> findByUserId(String userId);

    /** 한 페이지에 대한 요청 사번들의 부여 행 — 부여/변경/무변화 분류용. */
    List<PagePermissionJpaEntity> findByPageCodeAndUserIdIn(String pageCode, Collection<String> userIds);

    /** 등급 변경 시 대상 행 로드. */
    Optional<PagePermissionJpaEntity> findByPageCodeAndUserId(String pageCode, String userId);

    /** 회수 — 삭제된 행 수(0 또는 1). uk(page_code, user_id)라 최대 1건. */
    long deleteByPageCodeAndUserId(String pageCode, String userId);
}
