package com.group3.vitamins.auth.application.usecase;

import com.group3.vitamins.auth.application.command.AgreeTermsCommand;
import com.group3.vitamins.auth.application.command.ChangePasswordCommand;
import com.group3.vitamins.auth.application.command.LoginCommand;
import com.group3.vitamins.auth.application.result.UserProfileRow;

/**
 * 인증 쓰기 인바운드 포트 — 로그인 · 비밀번호 변경 · 약관 동의 (`.ai/api/auth.md`).
 */
public interface AuthCommandUseCase {

    /** 로그인. 성공 시 로그인 응답·세션 수립에 필요한 프로필을 반환한다. */
    UserProfileRow login(LoginCommand command);

    void changePassword(ChangePasswordCommand command);

    void agreeTerms(AgreeTermsCommand command);
}
