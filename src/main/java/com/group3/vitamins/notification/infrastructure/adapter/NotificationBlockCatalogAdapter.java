package com.group3.vitamins.notification.infrastructure.adapter;

import com.group3.vitamins.notification.application.port.BlockRef;
import com.group3.vitamins.notification.application.port.BlockTypeLookupPort;
import com.group3.vitamins.project.block.domain.repository.BlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Block 도메인(동훈님 소관) 실 연동 — `approval.infrastructure.catalog.ApprovalBlockCatalogAdapter` 와
 * 동일하게 block 도메인의 조회 전용 리포지토리를 직접 재사용한다(로직 복제 없음).
 */
@Component
@RequiredArgsConstructor
public class NotificationBlockCatalogAdapter implements BlockTypeLookupPort {

    private final BlockRepository blockRepository;

    @Override
    public Optional<BlockRef> findBlock(Long blockId) {
        return blockRepository.findById(blockId)
                .map(block -> new BlockRef(block.getType().name(), block.getTypeId()));
    }
}
