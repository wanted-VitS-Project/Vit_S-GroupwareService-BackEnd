package com.group3.vitamins.vitamate.analysis.domain.repository;

import java.time.LocalDateTime;
import java.util.Optional;

// 비타메이트 블록 상세 행을 생성하고 삭제 상태로 바꾸는 저장 포트
public interface VitamateBlockRepository {

    // block 생성 트랜잭션 안에서 비어 있는 AI 블록 상세 행을 만든다.
    Long create(Long blockId);

    // 삭제 로그를 남길 수 있도록 상세 행이 속한 공통 block_id를 조회한다.
    Optional<Long> findBlockId(Long vitamateBlockId);

    // AI 블록 상세 행을 논리 삭제한다. 이미 삭제된 행이면 false를 반환한다.
    boolean markDeleted(Long vitamateBlockId, LocalDateTime deletedAt);
}
