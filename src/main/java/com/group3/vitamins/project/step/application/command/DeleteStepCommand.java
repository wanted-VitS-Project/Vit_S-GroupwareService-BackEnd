package com.group3.vitamins.project.step.application.command;

import java.util.List;

/**
 * 스텝 삭제 (STP-013). 하위 블록·이슈를 함께 논리 삭제하되,
 * {@code moveBlockIds} 로 지정한 블록만 {@code moveToStepId} 로 살려서 옮긴다.
 *
 * <p>⛔ 이슈는 선택지가 없다 — 무조건 함께 삭제된다 (STP-008 폐기).
 */
public record DeleteStepCommand(
        Long stepId,
        List<Long> moveBlockIds,
        Long moveToStepId,
        String requesterUserId,
        String role
) {
}
