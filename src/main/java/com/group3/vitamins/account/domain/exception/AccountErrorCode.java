package com.group3.vitamins.account.domain.exception;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Account 도메인 에러 코드.
 *
 * <p>⛔ <b>여기 있는 코드와 메시지는 프론트와의 계약이다</b> (`.ai/api/account.md` · 노션 확정).
 * 임의로 추가·변경하지 마라. 필요하면 노션을 먼저 고친다 (`.ai/API.md` §0).
 *
 * <p>401({@code AUTH_UNAUTHENTICATED}) 은 전 도메인 공통이라 여기 두지 않는다 —
 * {@code GlobalExceptionHandler} 가 Security 필터에서 넘어온 미인증을 직접 처리한다.
 */
@Getter
@RequiredArgsConstructor
public enum AccountErrorCode implements ErrorCode {

    // --- 400 ---
    /** `userIds` 가 비어 있음 (비밀번호 재설정) */
    ACC_INVALID_REQUEST("ACC_INVALID_REQUEST", "필수 입력값이 누락되었습니다."),
    ACC_INVALID_ROLE("ACC_INVALID_ROLE", "허용되지 않는 권한 값입니다."),
    /** ADMIN 은 개발자가 직접 발급한다. 이 API 로는 부여할 수 없다 */
    ACC_ADMIN_ROLE_NOT_ALLOWED("ACC_ADMIN_ROLE_NOT_ALLOWED", "ADMIN 권한은 이 API 로 부여할 수 없습니다."),
    /** 자기 자신의 role 행은 수정할 수 없다 (`PERMISSION.md` §2-3) */
    ACC_SELF_MODIFICATION_NOT_ALLOWED("ACC_SELF_MODIFICATION_NOT_ALLOWED",
            "자기 자신의 권한은 변경할 수 없습니다."),
    ACC_INVALID_STATUS("ACC_INVALID_STATUS", "허용되지 않는 상태 값입니다."),
    ACC_STATUS_UNCHANGED("ACC_STATUS_UNCHANGED", "이미 요청한 상태입니다."),

    // --- 403 ---
    ACC_ADMIN_REQUIRED("ACC_ADMIN_REQUIRED", "관리자 권한이 필요합니다."),
    /** 시스템 계정(ADMIN 가상 사원)은 role·status 변경 대상이 될 수 없다 (`EMP-003`) */
    ACC_SYSTEM_ACCOUNT_NOT_ALLOWED("ACC_SYSTEM_ACCOUNT_NOT_ALLOWED",
            "시스템 계정은 변경할 수 없습니다."),
    /** 비밀번호 재설정 대상에 ADMIN 계정이 포함됨 (`ACC-023` · `ACC-024`) */
    ACC_ADMIN_ACCOUNT_NOT_ALLOWED("ACC_ADMIN_ACCOUNT_NOT_ALLOWED",
            "ADMIN 계정은 비밀번호 재설정 대상이 될 수 없습니다."),

    // --- 404 ---
    ACC_NOT_FOUND("ACC_NOT_FOUND", "계정을 찾을 수 없습니다.");

    private final String code;
    private final String message;
}
