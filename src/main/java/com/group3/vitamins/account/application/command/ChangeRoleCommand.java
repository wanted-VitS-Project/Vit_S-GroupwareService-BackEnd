package com.group3.vitamins.account.application.command;

/**
 * 전역 권한 변경 커맨드 (`.ai/api/account.md` §1).
 *
 * @param actorUserId  요청자 사번 — 자기 자신 변경 차단 판정에 쓴다
 * @param actorRole    요청자 전역 권한 — ADMIN 판정에 쓴다 (세션에서 온다)
 * @param targetUserId 대상 사번
 * @param role         부여할 권한 (MASTER·MEMBER)
 */
public record ChangeRoleCommand(
        String actorUserId,
        String actorRole,
        String targetUserId,
        String role
) {
}
