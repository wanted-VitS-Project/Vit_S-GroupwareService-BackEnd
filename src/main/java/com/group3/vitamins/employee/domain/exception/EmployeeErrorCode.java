package com.group3.vitamins.employee.domain.exception;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사원 도메인 에러코드 (`.ai/api/employee.md`).
 *
 * <p>목록·상세(#121) + 등록(#122 PR-A)까지 구현돼 있다. 수정·퇴사(PR-B)의 코드는 그 시점에 추가한다.
 */
@Getter
@RequiredArgsConstructor
public enum EmployeeErrorCode implements ErrorCode {

    EMP_INVALID_PARAMETER("EMP_INVALID_PARAMETER",
            "요청 파라미터가 올바르지 않습니다."),

    EMP_NOT_FOUND("EMP_NOT_FOUND",
            "사원을 찾을 수 없습니다."),

    // ── 등록 (employee.md §3) ──
    EMP_INVALID_REQUEST("EMP_INVALID_REQUEST",
            "필수값이 누락되었거나 형식이 올바르지 않습니다."),

    EMP_ADMIN_ROLE_NOT_ALLOWED("EMP_ADMIN_ROLE_NOT_ALLOWED",
            "ADMIN 권한은 부여할 수 없습니다."),

    EMP_DEPARTMENT_NOT_FOUND("EMP_DEPARTMENT_NOT_FOUND",
            "부서를 찾을 수 없습니다."),

    EMP_JOB_POSITION_NOT_FOUND("EMP_JOB_POSITION_NOT_FOUND",
            "직급을 찾을 수 없습니다."),

    // ── 학력/자격증 마스터 참조 (employee.md §3·§4 · qualification.md) ──
    // 사원 학력/자격증이 참조하는 전공·자격증이 이 회사 마스터에 없을 때. 접두어는 마스터 도메인(MAJOR/CERT)을 따른다.
    MAJOR_NOT_FOUND("MAJOR_NOT_FOUND",
            "전공을 찾을 수 없습니다."),

    CERT_NOT_FOUND("CERT_NOT_FOUND",
            "자격증을 찾을 수 없습니다."),

    EMP_USER_ID_DUPLICATED("EMP_USER_ID_DUPLICATED",
            "이미 등록된 사번입니다."),

    // ── 퇴사 (employee.md §5) ──
    EMP_ALREADY_RESIGNED("EMP_ALREADY_RESIGNED",
            "이미 퇴사 처리된 사원입니다."),

    // ── 엑셀 일괄 등록 (employee.md §6~§8) ──
    // 파일 누락·5MB 초과는 업로드 단계(열기 전)의 400 이다. 형식 오류는 확장자(열기 전)뿐 아니라
    // 파싱 실패(손상 파일 등, 열기·파싱 중)에서도 나는 400 이다. 파일을 연 뒤의 <b>행별</b> 오류는
    // /bulk/validate 와 /bulk(skipErrors=true) 에서 200 + data.errors 로 나간다.
    EMP_FILE_REQUIRED("EMP_FILE_REQUIRED",
            "업로드할 파일이 없습니다."),

    EMP_FILE_TYPE_INVALID("EMP_FILE_TYPE_INVALID",
            "엑셀 파일(.xlsx · .xls)만 업로드할 수 있습니다."),

    EMP_FILE_SIZE_EXCEEDED("EMP_FILE_SIZE_EXCEEDED",
            "파일 크기가 5MB를 초과했습니다."),

    // /bulk(skipErrors=false) 는 예외 — 행 오류가 하나라도 있으면 파일을 연 뒤라도 등록을 전량 거부하는 400 이다.
    EMP_HAS_ERRORS("EMP_HAS_ERRORS",
            "오류 행이 있어 등록할 수 없습니다. 오류 제외 등록을 사용하세요."),

    // ── 프로필 사진 (auth.md §5-1·§5-2 · employee.md §10) ──
    // 업로드/삭제는 auth 마이페이지 경로지만 데이터가 사원 속성이라 사원 도메인이 소유한다 → EMP_ 접두어로 통일.
    EMP_PROFILE_IMAGE_REQUIRED("EMP_PROFILE_IMAGE_REQUIRED",
            "업로드할 이미지 파일이 없습니다."),

    EMP_PROFILE_IMAGE_TYPE_INVALID("EMP_PROFILE_IMAGE_TYPE_INVALID",
            "지원하지 않는 이미지 형식입니다. (jpg·jpeg·png·gif)"),

    EMP_PROFILE_IMAGE_SIZE_EXCEEDED("EMP_PROFILE_IMAGE_SIZE_EXCEEDED",
            "이미지 크기가 5MB를 초과했습니다."),

    EMP_PROFILE_IMAGE_NOT_FOUND("EMP_PROFILE_IMAGE_NOT_FOUND",
            "프로필 사진이 없습니다.");

    private final String code;
    private final String message;
}
