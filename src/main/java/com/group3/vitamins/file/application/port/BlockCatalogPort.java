package com.group3.vitamins.file.application.port;

import java.util.Optional;

/**
 * 블록(Workspace) 조회 아웃바운드 포트. 파일은 블록에 붙지만 블록 골격은 project.block 소유라
 * 직접 조인하지 않고 이 포트로 묻는다. 구현은 {@code infrastructure/adapter/BlockCatalogAdapter}.
 *
 * <p>권한(스텝 편집/열람 판정)은 이 포트가 아니라 {@code StepAccessUseCase} 가 담당한다 —
 * 여기서는 "블록이 살아있고 파일을 받을 수 있는 타입인가 + 그 스텝은 무엇인가" 만 해석한다.
 */
public interface BlockCatalogPort {

    /**
     * FILE 블록의 소유 스텝 ID 를 돌려준다. 블록이 없거나 soft delete 됐거나 FILE 타입이 아니면 empty
     * (호출 서비스가 {@code FILE_BLOCK_NOT_FOUND} 로 변환한다).
     *
     * <p>⛔ FILE 전용. 블록 파일 목록(§3)만 이 메서드를 쓴다 — 결재 블록에 매달린 파일은
     * 그 목록에 노출하지 않기 때문이다. 업로드·다운로드·버전조회·수정/삭제는
     * {@link #resolveAttachableBlockStepId(Long)} 를 쓴다.
     */
    Optional<Long> resolveFileBlockStepId(Long blockId);

    /**
     * 파일을 매달 수 있는 블록(FILE 또는 APPROVAL)의 소유 스텝 ID 를 돌려준다.
     * 블록이 없거나 soft delete 됐거나 두 타입이 아니면 empty
     * (호출 서비스가 {@code FILE_BLOCK_NOT_FOUND} 로 변환한다).
     *
     * <p>결재 블록 드롭존 업로드를 위해 대상 타입을 넓힌 것이다(`file.md §1`, 2026-08-06 확정).
     * 결재 도메인은 자체 업로드 API 를 두지 않고 공용 파일 API 를 재사용하며, 권한·삭제잠금·버전
     * 조회는 FILE 블록과 동일한 `블록→스텝` 경로를 그대로 탄다.
     */
    Optional<Long> resolveAttachableBlockStepId(Long blockId);

    /**
     * 결재({@code APPROVAL}) 블록이면 true. 블록이 없거나 soft delete 됐으면 false.
     *
     * <p>동명 문서 검사를 면제할지 판정하는 데만 쓴다(`file.md §1`, 2026-08-16 확정). 결재 문서
     * 제거는 {@code approval_document} 링크만 끊고 파일은 블록에 남는데, 그 파일은 블록 파일
     * 목록(§3)에서 제외돼 사용자가 발견·삭제할 경로가 없다 — 검사를 그대로 두면 "제거 후 같은
     * 파일 재첨부"가 영구히 409 로 막힌다.
     *
     * <p>존재·타입 판정은 {@link #resolveAttachableBlockStepId(Long)} 가 이미 끝낸 뒤 호출되므로
     * 여기서는 예외를 던지지 않고 boolean 만 돌려준다.
     */
    boolean isApprovalBlock(Long blockId);
}
