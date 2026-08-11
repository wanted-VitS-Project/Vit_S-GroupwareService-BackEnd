package com.group3.vitamins.businesscategory.application.port;

import java.util.Set;

/**
 * 프로젝트 도메인에 물어보는 아웃바운드 포트.
 * 카테고리 도메인은 이 인터페이스만 알고, 실제 조회는 infrastructure/adapter 구현체가 처리한다.
 *
 * <p>프로젝트↔카테고리 연결은 {@code project_business_category} 조인 테이블이 갖고 있고
 * 그 테이블은 프로젝트 도메인 소관이다. 프로젝트 도메인이 구현되면 어댑터만 갈아끼운다.
 */
public interface ProjectCategoryLinkPort {

    /**
     * 프로젝트에 연결된 카테고리 ID 전체를 조회한다.
     * 목록 항목마다 따로 세면 N+1 이 되므로 한 번에 받아 대조한다.
     */
    Set<Long> findLinkedCategoryIds(Long companyId);

    /** 같은 회사에서 이 카테고리에 연결된 프로젝트 수. DELETE 409 메시지에 건수를 담을 때 쓴다. */
    long countLinkedProjects(Long categoryId, Long companyId);
}