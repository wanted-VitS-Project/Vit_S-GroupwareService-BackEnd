package com.group3.vitamins.file.infrastructure.adapter;

import com.group3.vitamins.file.application.usecase.FileCommandUseCase;
import com.group3.vitamins.project.block.application.port.BlockFileTrashPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * block 도메인의 {@link BlockFileTrashPort} 를 file 도메인이 구현한다 (D안 · 2026-08-16).
 *
 * <p>cross-domain 구현 선례 {@code VitamateFileDerivedDataCleanupAdapter} 와 동형 — 포트는 block 이 선언하고
 * 실제 파일 휴지통 이동은 파일 도메인의 {@link FileCommandUseCase} 가 수행한다(호출부와 같은 트랜잭션).
 */
@Component
@RequiredArgsConstructor
public class BlockFileTrashAdapter implements BlockFileTrashPort {

    private final FileCommandUseCase fileCommandUseCase;

    @Override
    public int trashByBlockId(Long blockId, String actorUserId) {
        return fileCommandUseCase.trashByBlockDeletion(blockId, actorUserId);
    }
}
