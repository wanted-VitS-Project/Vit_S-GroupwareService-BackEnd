package com.group3.vitamins.account.application.usecase;

import com.group3.vitamins.account.application.command.ResetPasswordsCommand;
import com.group3.vitamins.account.application.result.PasswordResetResult;

/**
 * 비밀번호 일괄 재설정 인바운드 포트 (`.ai/api/account.md` §3). ADMIN 전용.
 *
 * <p>메일 발송 단계에서만 부분 실패를 허용하며, 실패가 섞여도 결과 집계를 반환한다(요청 자체는 성공).
 */
public interface AccountPasswordResetUseCase {

    PasswordResetResult resetPasswords(ResetPasswordsCommand command);
}
