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
     * 해당 프로젝트의 member 인지 확인한다(APR-012, `PERMISSION.md` §6).
     * 실제 `project_member` 조회로 구현됨(2026-08-05) — `ApprovalBlockCatalogAdapter` 참고.
     */
    boolean isProjectMember(Long projectId, String userId);

    /** 결재가 연결된 블록의 프로젝트가 요청 회사 소속인지 확인한다. */
    boolean isBlockInCompany(Long blockId, Long companyId);

    /** 원 기안자 참여 불가 시 대행 기안자를 선점할 수 있는 유효 스텝 EDITOR인지 확인한다. */
    boolean isStepEditor(Long blockId, String userId, String role);

    /**
     * 블록이 속한 스텝의 열람 권한(VIEWER 이상)이 있는지 확인한다(MGT-005, 2026-08-15 계약 변경).
     *
     * <p>판정 기준은 블록 목록조회({@code BlockQueryService})와 <b>같아야 한다</b> — 프로젝트 권한을
     * 스텝 오버라이드로 덮은 최종값이 {@code NONE} 이 아니면 통과. 여기서 {@code project_member} 행
     * 존재만 보면(=  {@link #isProjectMember}) 스텝 오버라이드로 {@code NONE} 이 걸린 사람이 통과해,
     * <b>블록 목록은 403 인데 그 안의 결재 상세만 열리는</b> 권한 누수가 생긴다.
     */
    boolean canViewBlock(Long blockId, String userId, String role);
}
