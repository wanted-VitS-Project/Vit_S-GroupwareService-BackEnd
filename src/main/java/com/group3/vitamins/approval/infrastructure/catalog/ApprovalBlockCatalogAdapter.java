package com.group3.vitamins.approval.infrastructure.catalog;

import com.group3.vitamins.approval.application.port.BlockCatalogPort;
import com.group3.vitamins.approval.application.port.BlockSummary;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Block/Project 도메인(동훈님 소관) 연동 지점. {@code block}·{@code step}·{@code project_member}
 * 테이블 조회 인프라가 아직 없어 임시로 항상 통과시킨다
 * (`text.infrastructure.catalog.CatalogBlockAdapter`와 동일한 임시 처리).
 */
@Component
public class ApprovalBlockCatalogAdapter implements BlockCatalogPort {

    private static final String APPROVAL_BLOCK_TYPE = "APPROVAL";

    @Override
    public Optional<BlockSummary> findBlock(Long blockId) {
        // TODO: 공용 block 테이블 조회 인프라가 아직 없어 임시로 항상 존재·APPROVAL 타입으로 간주한다.
        return Optional.of(new BlockSummary(blockId, APPROVAL_BLOCK_TYPE, null));
    }

    @Override
    public boolean isProjectMember(Long projectId, String userId) {
        // TODO: project_member 조회 인프라가 아직 없어 임시로 항상 true 를 반환한다.
        return true;
    }
}
