package com.group3.vitamins.file.domain.exception;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 파일 도메인 에러코드 (`.ai/api/file.md`).
 *
 * <p>⚠️ 401 {@code AUTH_UNAUTHENTICATED} 은 전역 인증 진입점이 처리하므로 여기 없다.
 * 파일은 스텝 권한을 따르므로 403 도 파일 전용 코드({@code FILE_ACCESS_PERMISSION_REQUIRED}·
 * {@code FILE_EDIT_PERMISSION_REQUIRED})를 쓴다 — job-position 처럼 account 코드를 재사용하지 않는다.
 *
 * <p>§7 영구삭제(2026-08-07 구현)에서 {@code FILE_NOT_DELETED}·{@code FILE_CONFIRM_TEXT_MISMATCH}·
 * {@code FILE_APPROVAL_REFERENCED} 를 추가했다. §6 복구는 그 엔드포인트 구현 시점에 채운다.
 */
@Getter
@RequiredArgsConstructor
public enum FileErrorCode implements ErrorCode {

    // --- 공통 입력 ---
    FILE_INVALID_REQUEST("FILE_INVALID_REQUEST",
            "요청 형식이 올바르지 않습니다."),
    FILE_SIZE_EXCEEDED("FILE_SIZE_EXCEEDED",
            "파일 크기가 50MB를 초과했습니다."),
    FILE_EXTENSION_BLOCKED("FILE_EXTENSION_BLOCKED",
            "업로드할 수 없는 실행 파일 확장자입니다."),

    // --- 귀속 (입찰 검토 파일 귀속 · FILE-V1 §2-G) ---
    FILE_REQUESTER_NOT_EMPLOYEE("FILE_REQUESTER_NOT_EMPLOYEE",
            "요청자가 임직원이 아니어서 파일 귀속을 처리할 수 없습니다."),

    // --- 권한 (스텝 권한을 따른다) ---
    FILE_ACCESS_PERMISSION_REQUIRED("FILE_ACCESS_PERMISSION_REQUIRED",
            "스텝 열람 권한이 없습니다."),
    FILE_EDIT_PERMISSION_REQUIRED("FILE_EDIT_PERMISSION_REQUIRED",
            "스텝 편집 권한이 없습니다."),

    // --- 조회 실패 ---
    FILE_BLOCK_NOT_FOUND("FILE_BLOCK_NOT_FOUND",
            "블록이 없거나 삭제되었습니다."),
    FILE_NOT_FOUND("FILE_NOT_FOUND",
            "문서를 찾을 수 없습니다."),
    FILE_VERSION_NOT_FOUND("FILE_VERSION_NOT_FOUND",
            "파일 버전을 찾을 수 없습니다."),

    // --- 업로드 완료 통보 검증 ---
    FILE_ALREADY_COMPLETED("FILE_ALREADY_COMPLETED",
            "이미 완료된 버전입니다."),
    FILE_OBJECT_NOT_FOUND("FILE_OBJECT_NOT_FOUND",
            "저장소에 업로드된 객체가 없습니다."),
    FILE_SIZE_MISMATCH("FILE_SIZE_MISMATCH",
            "업로드된 파일 크기가 요청과 다릅니다."),
    FILE_CHECKSUM_MISMATCH("FILE_CHECKSUM_MISMATCH",
            "업로드된 파일 체크섬이 일치하지 않습니다."),
    FILE_UPLOAD_NOT_COMPLETED("FILE_UPLOAD_NOT_COMPLETED",
            "업로드가 완료되지 않은 버전입니다."),

    // --- 이름 중복 (블록 내) ---
    FILE_NAME_DUPLICATED("FILE_NAME_DUPLICATED",
            "같은 이름의 문서가 이미 존재합니다."),

    // --- 동시수정 충돌 (낙관락 · §4) ---
    FILE_VERSION_CONFLICT("FILE_VERSION_CONFLICT",
            "다른 사용자가 먼저 수정했습니다."),

    // --- 삭제 (휴지통 이동) ---
    FILE_ALREADY_DELETED("FILE_ALREADY_DELETED",
            "이미 휴지통에 있는 문서입니다."),
    FILE_APPROVAL_IN_PROGRESS("FILE_APPROVAL_IN_PROGRESS",
            "진행 중인 결재의 대상이라 삭제할 수 없습니다."),

    // --- 영구 삭제 (§7) ---
    FILE_NOT_DELETED("FILE_NOT_DELETED",
            "휴지통에 있는 문서만 영구 삭제할 수 있습니다."),
    FILE_CONFIRM_TEXT_MISMATCH("FILE_CONFIRM_TEXT_MISMATCH",
            "확인 문자가 일치하지 않습니다."),
    FILE_APPROVAL_REFERENCED("FILE_APPROVAL_REFERENCED",
            "결재가 이 문서의 버전을 참조하고 있어 영구 삭제할 수 없습니다."),

    // --- 미리보기 ---
    FILE_PREVIEW_NOT_SUPPORTED("FILE_PREVIEW_NOT_SUPPORTED",
            "PDF 만 미리보기를 지원합니다."),
    FILE_PREVIEW_FAILED("FILE_PREVIEW_FAILED",
            "미리보기 생성에 실패했습니다.");

    private final String code;
    private final String message;
}
