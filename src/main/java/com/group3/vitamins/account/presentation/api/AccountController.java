package com.group3.vitamins.account.presentation.api;

import com.group3.vitamins.account.application.AccountPasswordResetService;
import com.group3.vitamins.account.application.AccountService;
import com.group3.vitamins.account.presentation.api.dto.request.ChangeRoleRequest;
import com.group3.vitamins.account.presentation.api.dto.request.ChangeStatusRequest;
import com.group3.vitamins.account.presentation.api.dto.request.ResetPasswordRequest;
import com.group3.vitamins.account.presentation.api.dto.response.PasswordResetResponse;
import com.group3.vitamins.account.presentation.api.dto.response.RoleChangeResponse;
import com.group3.vitamins.account.presentation.api.dto.response.StatusChangeResponse;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 계정 관리 API — `.ai/api/account.md` (노션 확정).
 *
 * <p>전부 <b>ADMIN 전용</b>이다. 인증(세션)은 Security 필터가, ADMIN 판정은 서비스가
 * 도메인 코드({@code ACC_ADMIN_REQUIRED})와 함께 한다.
 *
 * <p>사람을 가리키는 식별자는 언제나 사번({@code userId})이다 — {@code account_id} 는 외부로 나가지 않는다.
 */
@Tag(name = "Account - 계정 관리", description = "전역 권한 변경 / 계정 상태 변경 / 비밀번호 재설정 (ADMIN 전용, 담당: 김동현)")
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private static final String ROLE_PREFIX = "ROLE_";

    private final AccountService accountService;
    private final AccountPasswordResetService accountPasswordResetService;

    @Operation(summary = "전역 권한 변경",
            description = "대상 사번의 전역 권한을 MASTER 또는 MEMBER 로 변경한다. "
                    + "ADMIN 은 이 API 로 부여할 수 없고, 자기 자신·시스템 계정도 대상이 될 수 없다. "
                    + "변경 후 대상의 세션을 종료해 재로그인 시 새 권한이 즉시 반영된다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "ACC_INVALID_ROLE — 허용되지 않는 값 · "
                            + "ACC_ADMIN_ROLE_NOT_ALLOWED — ADMIN 부여 시도 · "
                            + "ACC_SELF_MODIFICATION_NOT_ALLOWED — 자기 자신 변경"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "ACC_ADMIN_REQUIRED — ADMIN 아님 · "
                            + "ACC_SYSTEM_ACCOUNT_NOT_ALLOWED — 시스템 계정 대상"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "ACC_NOT_FOUND — 계정 없음")
    })
    @PatchMapping("/{userId}/role")
    public ApiResponse<RoleChangeResponse> changeRole(@AuthenticationPrincipal String currentUserId,
                                                      Authentication authentication,
                                                      @PathVariable String userId,
                                                      @RequestBody ChangeRoleRequest request) {
        accountService.changeRole(currentUserId, roleOf(authentication), userId, request.role());
        return ApiResponse.success("권한이 변경되었습니다.", new RoleChangeResponse(userId, request.role()));
    }

    @Operation(summary = "계정 상태 변경",
            description = "대상 계정을 ACTIVE 또는 INACTIVE 로 토글한다. 시스템 계정은 대상이 될 수 없다. "
                    + "비활성화 시 대상의 세션을 즉시 종료한다. (퇴사 처리와는 별개 API 다)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "ACC_INVALID_STATUS — 허용되지 않는 값 · ACC_STATUS_UNCHANGED — 이미 같은 상태"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "ACC_ADMIN_REQUIRED — ADMIN 아님 · "
                            + "ACC_SYSTEM_ACCOUNT_NOT_ALLOWED — 시스템 계정 대상"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "ACC_NOT_FOUND — 계정 없음")
    })
    @PatchMapping("/{userId}/status")
    public ApiResponse<StatusChangeResponse> changeStatus(Authentication authentication,
                                                          @PathVariable String userId,
                                                          @RequestBody ChangeStatusRequest request) {
        accountService.changeStatus(roleOf(authentication), userId, request.status());
        return ApiResponse.success("계정 상태가 변경되었습니다.", new StatusChangeResponse(userId, request.status()));
    }

    @Operation(summary = "비밀번호 재설정",
            description = "대상 사번 목록의 비밀번호를 임시 비밀번호로 재설정하고 이메일로 발송한다. "
                    + "개인·다중이 같은 API 다(1명이면 길이 1 배열). 존재하지 않는 사번·ADMIN 이 섞이면 전체 거부하고, "
                    + "메일 발송 단계에서만 부분 실패를 허용한다. 실패가 섞여 있어도 HTTP 200 이며 집계를 반환한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "처리 완료 (부분 실패 포함). data.failures[] 로 집계를 반환"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "ACC_INVALID_REQUEST — userIds 가 비어 있음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "ACC_ADMIN_REQUIRED — ADMIN 아님 · "
                            + "ACC_ADMIN_ACCOUNT_NOT_ALLOWED — 대상에 ADMIN 계정 포함"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "ACC_NOT_FOUND — 존재하지 않는 사번 포함 (전체 거부)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503",
                    description = "AUTH_HASHING_BUSY — 해시 동시 실행 한도 초과 (서버 과부하). 잠시 후 재시도")
    })
    @PostMapping("/password-resets")
    public ApiResponse<PasswordResetResponse> resetPasswords(Authentication authentication,
                                                             @RequestBody ResetPasswordRequest request) {
        PasswordResetResponse result =
                accountPasswordResetService.resetPasswords(roleOf(authentication), request.userIds());
        return ApiResponse.success("비밀번호 재설정 완료", result);
    }

    /**
     * 현재 로그인 사용자의 전역 권한을 추출한다.
     *
     * <p>세션에는 {@code ROLE_ADMIN} 처럼 접두어가 붙은 권한이 실려 있다 (`AuthSessionManager`).
     * 접두어를 떼어 {@code ADMIN}·{@code MASTER}·{@code MEMBER} 원값으로 돌려준다.
     */
    private String roleOf(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith(ROLE_PREFIX))
                .map(auth -> auth.substring(ROLE_PREFIX.length()))
                .findFirst()
                .orElse(null);
    }
}
