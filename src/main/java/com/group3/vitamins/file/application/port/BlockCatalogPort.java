package com.group3.vitamins.file.application.port;

import java.util.Optional;

/**
 * 블록(Workspace) 조회 아웃바운드 포트. 파일은 블록에 붙지만 블록 골격은 project.block 소유라
 * 직접 조인하지 않고 이 포트로 묻는다. 구현은 {@code infrastructure/adapter/BlockCatalogAdapter}.
 *
 * <p>권한(스텝 편집/열람 판정)은 이 포트가 아니라 {@code StepAccessUseCase} 가 담당한다 —
 * 여기서는 "블록이 살아있는 FILE 블록인가 + 그 스텝은 무엇인가" 만 해석한다.
 */
public interface BlockCatalogPort {

    /**
     * FILE 블록의 소유 스텝 ID 를 돌려준다. 블록이 없거나 soft delete 됐거나 FILE 타입이 아니면 empty
     * (호출 서비스가 {@code FILE_BLOCK_NOT_FOUND} 로 변환한다).
     */
    Optional<Long> resolveFileBlockStepId(Long blockId);
}
