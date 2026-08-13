package com.group3.vitamins.project.block.application.command;

/**
 * 블록 삭제 요청.
 *
 * <p>{@code confirmApprovalCancel} 은 상세 타입의 부작용을 사용자가 확인했다는 표시다 (DEL-016).
 * 결재 블록은 상신 이후면 첫 요청을 409 로 되묻고, 이 값이 {@code true} 인 재요청만 삭제한다.
 * 상태 개념이 없는 타입에는 영향이 없다.
 */
public record DeleteBlockCommand(
        Long blockId,
        String requesterUserId,
        String role,
        boolean confirmApprovalCancel
) {
}
