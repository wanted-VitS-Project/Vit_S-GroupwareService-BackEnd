package com.group3.vitamins.approval.application.port;

import java.util.Optional;

/**
 * Block/Project 도메인(동훈님 소관)에 물어보는 아웃바운드 포트.
 * 결재 도메인은 이 인터페이스만 알고, 실제 조회는 infrastructure/catalog 구현체가 처리한다
 * (`text.application.port.BlockCatalogPort`와 동일 구조 — 도메인마다 자기 몫의 포트를 따로 둔다).
 */
public interface BlockCatalogPort {

    /** blockId 로 공용 block 테이블을 조회한다(존재 여부는 이 결과의 존재 자체로 판단) */
    Optional<BlockSummary> findBlock(Long blockId);

    /**
     * 해당 프로젝트의 member 인지 확인한다(`approval.md` 1번 요구사항 BND-001, `PERMISSION.md` §6).
     * TODO: project_member 조회 인프라가 아직 없어 임시로 항상 true 를 반환한다
     * (`text.infrastructure.catalog.CatalogBlockAdapter`와 동일한 임시 처리).
     */
    boolean isProjectMember(Long projectId, String userId);
}
