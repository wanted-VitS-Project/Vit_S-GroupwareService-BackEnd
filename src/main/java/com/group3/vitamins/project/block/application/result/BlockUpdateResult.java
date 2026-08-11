package com.group3.vitamins.project.block.application.result;

import java.time.LocalDateTime;

/**
 * 제목·담당자 수정 결과. 해제했으면 title·owner 가 null 이다.
 *
 * @param version 저장 후의 새 버전. 프론트는 이 값으로 화면 상태를 교체해야
 *                <b>다음 저장이 409 가 되지 않는다</b>
 */
public record BlockUpdateResult(
        Long blockId,
        String title,
        BlockOwner owner,
        LocalDateTime updatedAt,
        int version
) {
}
