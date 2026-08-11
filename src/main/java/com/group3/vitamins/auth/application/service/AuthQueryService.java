package com.group3.vitamins.auth.application.service;

import com.group3.vitamins.auth.application.port.UserProfileQueryPort;
import com.group3.vitamins.auth.application.result.UserProfileRow;
import com.group3.vitamins.auth.application.usecase.AuthQueryUseCase;
import com.group3.vitamins.auth.domain.exception.AuthErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 조회 유스케이스 — 내 정보 (`.ai/api/auth.md` §3).
 */
@Service
@RequiredArgsConstructor
public class AuthQueryService implements AuthQueryUseCase {

    private final UserProfileQueryPort userProfileQueryPort;

    @Override
    @Transactional(readOnly = true)
    public UserProfileRow loadProfile(String userId) {
        return userProfileQueryPort.findProfile(userId)
                // 세션은 살아 있는데 계정이 사라진 경우. 재로그인시키는 게 맞다.
                .orElseThrow(() -> new UnauthorizedException(AuthErrorCode.AUTH_UNAUTHENTICATED));
    }
}
