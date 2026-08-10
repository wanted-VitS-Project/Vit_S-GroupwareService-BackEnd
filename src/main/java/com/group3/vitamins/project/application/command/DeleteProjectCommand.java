package com.group3.vitamins.project.application.command;

/**
 * 프로젝트 삭제 (PRJ-014). 진행 전이고 스텝이 0개일 때만 논리 삭제된다 —
 * 이미 굴러간 프로젝트는 삭제가 아니라 <b>종결</b>로 처리한다.
 */
public record DeleteProjectCommand(
        Long projectId,
        String requesterUserId,
        String role
) {
}
