package com.group3.vitamins.account.application.result;

/**
 * 계정 관리 대상 사원의 검증·표시용 스냅샷.
 *
 * <p>{@code account} 와 {@code employee} 를 조인한 조회 결과다
 * ({@link com.group3.vitamins.account.application.port.AccountQueryPort} 의 반환 타입).
 * 쓰기는 {@code AccountEntity}(JPA)가 맡고, 이 행은 <b>판정과 응답 표시</b>(이름·이메일)에만 쓴다.
 *
 * @param userId   사번
 * @param name     이름 (재설정 실패 목록 표시용)
 * @param email    이메일 (없으면 {@code null} — 비밀번호 재설정에서 발송 대상 판정)
 * @param role     전역 권한 (ADMIN 대상 차단 판정)
 * @param isSystem 시스템 계정(ADMIN 가상 사원) 여부
 */
public record AccountTargetRow(
        String userId,
        String name,
        String email,
        String role,
        boolean isSystem
) {
}
