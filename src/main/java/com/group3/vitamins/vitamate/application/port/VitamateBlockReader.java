package com.group3.vitamins.vitamate.application.port;

import java.util.Optional;

// 비타메이트 블록과 프로젝트 접근 컨텍스트를 조회하는 포트
public interface VitamateBlockReader {

    Optional<VitamateBlockContext> findAccessibleVitamateBlock(Long blockId, String userId);

    record VitamateBlockContext(
            Long blockId,
            Long vitamateBlockId,
            Long stepId,
            Long projectId
    ) {
    }
}
