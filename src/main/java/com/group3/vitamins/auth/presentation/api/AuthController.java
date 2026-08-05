package com.group3.vitamins.auth.presentation.api;

import com.group3.vitamins.auth.application.command.AgreeTermsCommand;
import com.group3.vitamins.auth.application.result.UserProfileRow;
import com.group3.vitamins.auth.application.usecase.AuthCommandUseCase;
import com.group3.vitamins.auth.application.usecase.AuthQueryUseCase;
import com.group3.vitamins.auth.infrastructure.ratelimit.LoginRateLimiter;
import com.group3.vitamins.auth.infrastructure.web.AuthSessionManager;
import com.group3.vitamins.auth.presentation.api.request.ChangePasswordRequest;
import com.group3.vitamins.auth.presentation.api.request.LoginRequest;
import com.group3.vitamins.auth.presentation.api.response.LoginResponse;
import com.group3.vitamins.auth.presentation.api.response.MyInfoResponse;
import com.group3.vitamins.global.infrastructure.web.ClientIpResolver;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 API — `.ai/api/auth.md`.
 *
 * <p>인증 수단은 <b>HttpOnly 세션 쿠키</b>다. 응답 본문에 토큰이 없고 재발급 API 도 없다.
 * 프론트는 요청에 {@code credentials: 'include'} 만 켜면 된다.
 */
@Tag(name = "Auth - 인증", description = "로그인 / 로그아웃 / 내 정보 / 비밀번호 변경 (담당: 김동현)")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthCommandUseCase authCommandUseCase;
    private final AuthQueryUseCase authQueryUseCase;
    private final AuthSessionManager authSessionManager;
    private final LoginRateLimiter loginRateLimiter;
    private final ClientIpResolver clientIpResolver;

    @Operation(summary = "로그인",
            description = "사번 + 비밀번호로 인증하고 세션 쿠키를 발급한다. "
                    + "비밀번호 해시(Argon2id) 때문에 응답에 0.3~0.5초가 걸리는 것이 정상이다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "AUTH_INVALID_REQUEST — 사번 또는 비밀번호가 비어 있음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_LOGIN_FAILED — 사번 또는 비밀번호 불일치 (사번 존재 여부를 구분하지 않는다)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "AUTH_ACCOUNT_INACTIVE — 계정 비활성 (관리자만 해제 가능)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "423",
                    description = "AUTH_ACCOUNT_LOCKED — 실패 누적 잠금. message 에 해제 시각을 담는다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429",
                    description = "AUTH_TOO_MANY_REQUESTS — 같은 IP 에서 요청 과다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503",
                    description = "AUTH_HASHING_BUSY — 서버 과부하. 잠시 후 재시도")
    })
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                            HttpServletRequest httpRequest,
                                            HttpServletResponse httpResponse) {
        // ⚠️ 반드시 해시 이전에. 뒤에 두면 이미 64MB 를 쓴 뒤라 방어가 되지 않는다.
        loginRateLimiter.check(clientIpResolver.resolve(httpRequest));

        UserProfileRow profile = authCommandUseCase.login(request.toCommand());
        authSessionManager.openSession(
                profile.userId(), profile.role(),
                profile.termsAgreementRequired(), profile.mustChangePassword(),
                httpRequest, httpResponse);

        return ApiResponse.success(AuthResponseMessage.LOGIN_SUCCESS, LoginResponse.from(profile));
    }

    @Operation(summary = "로그아웃", description = "세션을 종료하고 쿠키를 만료시킨다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료")
    })
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest httpRequest) {
        authSessionManager.closeCurrentSession(httpRequest);
        return ApiResponse.success(AuthResponseMessage.LOGOUT_SUCCESS);
    }

    @Operation(summary = "내 정보 조회", description = "마이페이지가 쓰는 필드 전체를 반환한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료")
    })
    @GetMapping("/me")
    public ApiResponse<MyInfoResponse> myInfo(@AuthenticationPrincipal String userId) {
        return ApiResponse.success(AuthResponseMessage.MY_INFO_SUCCESS,
                MyInfoResponse.from(authQueryUseCase.loadProfile(userId)));
    }

    @Operation(summary = "약관 동의",
            description = "최초 로그인 시 이용약관·개인정보처리방침에 동의한다(1회성). 동의 후 비밀번호 변경으로 넘어간다. "
                    + "재설정 후 로그인은 약관을 다시 받지 않으며, ADMIN 은 대상이 아니다. 요청 본문 없음.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "동의 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료")
    })
    @PostMapping("/terms-agreements")
    public ApiResponse<Void> agreeTerms(@AuthenticationPrincipal String userId,
                                        HttpServletRequest httpRequest) {
        authCommandUseCase.agreeTerms(new AgreeTermsCommand(userId));
        // 약관 게이트 해제. 세션은 유지한다 — 이후 비밀번호 변경 게이트가 이어받는다
        authSessionManager.clearTermsAgreementFlag(httpRequest);
        return ApiResponse.success(AuthResponseMessage.TERMS_AGREED);
    }

    @Operation(summary = "비밀번호 변경",
            description = "본인 비밀번호만 변경할 수 있다. 최초 변경(passwordStatus=RESET_REQUIRED)이면 "
                    + "currentPassword 를 생략한다. 정책: 8자 이상 + 영문·숫자·특수문자 모두 포함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "AUTH_INVALID_REQUEST · AUTH_CURRENT_PASSWORD_REQUIRED · "
                            + "AUTH_CURRENT_PASSWORD_INVALID · AUTH_PASSWORD_CONFIRM_MISMATCH · "
                            + "AUTH_PASSWORD_POLICY_VIOLATION · AUTH_PASSWORD_UNCHANGED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료")
    })
    @PatchMapping("/password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal String userId,
                                            @Valid @RequestBody ChangePasswordRequest request,
                                            HttpServletRequest httpRequest) {
        authCommandUseCase.changePassword(request.toCommand(userId));
        // 게이트 해제. 세션은 유지한다 — 명세가 재로그인을 요구하지 않는다
        authSessionManager.clearPasswordResetFlag(httpRequest);
        return ApiResponse.success(AuthResponseMessage.PASSWORD_CHANGED);
    }
}
