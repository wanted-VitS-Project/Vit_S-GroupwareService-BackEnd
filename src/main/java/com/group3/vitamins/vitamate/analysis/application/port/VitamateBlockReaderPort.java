package com.group3.vitamins.vitamate.analysis.application.port;

import java.util.Optional;

// 비타메이트 블록과 프로젝트 접근 컨텍스트를 조회하는 포트
public interface VitamateBlockReaderPort {

    // 요청자가 접근 가능한 비타메이트 블록이면 블록과 프로젝트 컨텍스트를 반환한다.
    Optional<VitamateBlockContext> findAccessibleVitamateBlock(Long blockId, String userId);

    record VitamateBlockContext(
            Long blockId,
            Long vitamateBlockId,
            Long stepId,
            Long projectId
    ) {
    }
}
