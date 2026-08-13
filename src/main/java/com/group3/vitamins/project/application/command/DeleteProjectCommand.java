package com.group3.vitamins.project.application.command;

/**
 * 프로젝트 삭제 (PRJ-014). 스텝·블록·이슈까지 함께 논리 삭제된다.
 *
 * <p>{@code confirm} 은 <b>지워질 범위를 사용자가 확인했다</b>는 표시다. 진행 전이 아니거나 스텝이
 * 남아 있으면 첫 요청을 409 로 되묻고, {@code confirm=true} 재요청이면 그대로 지운다 —
 * <b>영구히 막지 않는다</b> (DEL-016 결재 블록 삭제와 같은 패턴).
 */
public record DeleteProjectCommand(
        Long projectId,
        String requesterUserId,
        String role,
        boolean confirm
) {
}
