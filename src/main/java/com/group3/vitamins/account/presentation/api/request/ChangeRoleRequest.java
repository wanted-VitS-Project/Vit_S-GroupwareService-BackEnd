package com.group3.vitamins.account.presentation.api.request;

import com.group3.vitamins.account.application.command.ChangeRoleCommand;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 전역 권한 변경 요청 (`.ai/api/account.md` §1).
 *
 * <p>값 검증(허용 범위·ADMIN 차단)은 서비스에서 도메인 코드({@code ACC_INVALID_ROLE} ·
 * {@code ACC_ADMIN_ROLE_NOT_ALLOWED})로 한다. Bean Validation 을 걸면 명세에 없는 코드가 나간다.
 */
public record ChangeRoleRequest(
        @Schema(description = "부여할 전역 권한", allowableValues = {"MASTER", "MEMBER"}, example = "MASTER")
        String role
) {

    /** 요청을 서비스 커맨드로 옮긴다. 요청자·대상 식별자는 세션·경로에서 온다. */
    public ChangeRoleCommand toCommand(String actorUserId, String actorRole, String targetUserId) {
        return new ChangeRoleCommand(actorUserId, actorRole, targetUserId, role);
    }
}
