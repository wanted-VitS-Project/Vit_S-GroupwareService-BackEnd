package com.group3.vitamins.auth.application.usecase;

import com.group3.vitamins.auth.application.result.UserProfileRow;

/**
 * 인증 조회 인바운드 포트 — 내 정보 (`.ai/api/auth.md` §3).
 */
public interface AuthQueryUseCase {

    /** 세션 사번으로 프로필을 조회한다. 계정이 사라졌으면 미인증으로 취급한다. */
    UserProfileRow loadProfile(String userId);
}
