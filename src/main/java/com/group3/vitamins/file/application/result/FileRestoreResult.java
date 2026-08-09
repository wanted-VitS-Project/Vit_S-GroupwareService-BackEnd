package com.group3.vitamins.file.application.result;

/**
 * 복구 결과(§6). 원래 블록이 살아있으면 {@code blockId} 로 돌아가고,
 * 삭제됐으면 {@code blockId=null}·{@code blockDeleted=true}(프로젝트 문서함으로 복구).
 */
public record FileRestoreResult(
        Long fileId,
        String name,
        Long blockId,
        boolean blockDeleted
) {
}
