package com.group3.vitamins.project.application.port;

import java.util.List;

/**
 * 카테고리 도메인에 물어보는 아웃바운드 포트.
 * 프로젝트 도메인은 이 인터페이스만 알고, 실제 조회는 infrastructure/adapter 구현체가 처리한다.
 */
public interface BusinessCategoryLookupPort {

    /** id 로 조회되는 카테고리만 반환한다 (없는 id 는 결과에서 빠진다 — 호출부가 개수로 판단). */
    List<BusinessCategoryView> findByIds(List<Long> categoryIds);

    record BusinessCategoryView(Long categoryId, String name) {}
}