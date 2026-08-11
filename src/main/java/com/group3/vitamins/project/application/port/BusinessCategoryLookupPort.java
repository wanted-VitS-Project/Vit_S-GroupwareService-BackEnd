package com.group3.vitamins.project.application.port;

import java.util.List;

/**
 * 카테고리 도메인에 물어보는 아웃바운드 포트.
 * 프로젝트 도메인은 이 인터페이스만 알고, 실제 조회는 infrastructure/adapter 구현체가 처리한다.
 */
public interface BusinessCategoryLookupPort {

    /**
     * <b>쓰기 경로의 존재 검증용</b> — 회사 범위에서 id 로 조회되는 <b>살아있는</b> 카테고리만 반환한다
     * (없는 id·타사·삭제된 카테고리는 결과에서 빠진다 — 호출부가 개수로 판단해 404 를 낸다).
     * 삭제된 카테고리를 새로 연결하면 안 되므로 이게 맞다.
     */
    List<BusinessCategoryView> findByIds(List<Long> categoryIds, Long companyId);

    /**
     * <b>조회·응답용</b> — 이미 연결된 카테고리를 옮겨 담는다.
     *
     * <p>⚠️ {@link #findByIds} 와 달리 <b>삭제된 카테고리도 반환한다</b>. 빼면 이미 연결돼 있던
     * 카테고리가 응답에서 조용히 사라지고, 개수로 404 를 내는 검증에 태우면 <b>연결 API 자체가 막힌다</b>
     * ({@code .ai/docs/global/DELETE.md} D-6).
     */
    List<BusinessCategoryRef> findRefsByIds(List<Long> categoryIds, Long companyId);

    record BusinessCategoryView(Long categoryId, String name, String code) {}

    /** @param deleted 카테고리가 논리 삭제됐는지. 삭제됐어도 이름·업무코드는 그대로 담는다 */
    record BusinessCategoryRef(Long categoryId, String name, String code, boolean deleted) {}
}