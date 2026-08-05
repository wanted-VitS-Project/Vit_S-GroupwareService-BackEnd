package com.group3.vitamins.account.application.command;

/**
 * 계정 상태 변경 커맨드 (`.ai/api/account.md` §2).
 *
 * @param actorRole    요청자 전역 권한 — ADMIN 판정에 쓴다
 * @param targetUserId 대상 사번
 * @param status       변경할 상태 (ACTIVE·INACTIVE)
 */
public record ChangeStatusCommand(
        String actorRole,
        String targetUserId,
        String status
) {
}
