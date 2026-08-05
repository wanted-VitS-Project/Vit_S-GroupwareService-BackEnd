package com.group3.vitamins.account.application.result;

import com.group3.vitamins.account.domain.PasswordResetFailureReason;

/**
 * 비밀번호 재설정 부분 실패 1건 (application 결과).
 *
 * <p>실패 사유는 도메인 enum({@link PasswordResetFailureReason}) 그대로 담는다 —
 * {@code passwordChanged} 파생과 문자열 노출은 프레젠테이션 응답 변환에서 한다.
 *
 * @param userId 사번
 * @param name   이름 (실패 목록 표시용)
 * @param reason 실패 사유
 */
public record PasswordResetFailureResult(
        String userId,
        String name,
        PasswordResetFailureReason reason
) {
}
