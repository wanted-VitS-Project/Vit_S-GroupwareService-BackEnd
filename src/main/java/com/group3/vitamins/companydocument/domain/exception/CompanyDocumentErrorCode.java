package com.group3.vitamins.companydocument.domain.exception;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사내 문서 도메인 에러코드 (`.ai/api/company-document.md`).
 *
 * <p>⚠️ 401 {@code AUTH_UNAUTHENTICATED} 은 전역 인증 진입점이, 403 은 ADMIN 정책이
 * {@code AccountErrorCode.ACC_ADMIN_REQUIRED} 를 던진다(file 은 스텝 권한이라 자체 403 코드를 쓰지만,
 * 사내 문서는 회사 단위 ADMIN 이라 account 코드를 재사용한다). 그래서 여기에 권한 코드는 없다.
 */
@Getter
@RequiredArgsConstructor
public enum CompanyDocumentErrorCode implements ErrorCode {

    // --- 공통 입력 ---
    CDOC_INVALID_REQUEST("CDOC_INVALID_REQUEST",
            "요청 형식이 올바르지 않습니다."),
    CDOC_SIZE_EXCEEDED("CDOC_SIZE_EXCEEDED",
            "파일 크기가 50MB를 초과했습니다."),
    CDOC_EXTENSION_BLOCKED("CDOC_EXTENSION_BLOCKED",
            "업로드할 수 없는 실행 파일 확장자입니다."),

    // --- 조회 실패 ---
    CDOC_NOT_FOUND("CDOC_NOT_FOUND",
            "문서를 찾을 수 없습니다."),
    CDOC_VERSION_NOT_FOUND("CDOC_VERSION_NOT_FOUND",
            "문서 버전을 찾을 수 없습니다."),

    // --- 업로드 완료 통보 검증 ---
    CDOC_ALREADY_COMPLETED("CDOC_ALREADY_COMPLETED",
            "이미 완료된 버전입니다."),
    CDOC_OBJECT_NOT_FOUND("CDOC_OBJECT_NOT_FOUND",
            "저장소에 업로드된 객체가 없습니다."),
    CDOC_SIZE_MISMATCH("CDOC_SIZE_MISMATCH",
            "업로드된 파일 크기가 요청과 다릅니다."),
    CDOC_CHECKSUM_MISMATCH("CDOC_CHECKSUM_MISMATCH",
            "업로드된 파일 체크섬이 일치하지 않습니다."),
    CDOC_UPLOAD_NOT_COMPLETED("CDOC_UPLOAD_NOT_COMPLETED",
            "업로드가 완료되지 않은 버전입니다."),

    // --- 삭제 · 복구 (soft delete) ---
    CDOC_ALREADY_DELETED("CDOC_ALREADY_DELETED",
            "이미 삭제된 문서입니다."),
    CDOC_NOT_DELETED("CDOC_NOT_DELETED",
            "삭제된 문서만 복구할 수 있습니다."),

    // --- 미리보기 ---
    CDOC_PREVIEW_NOT_SUPPORTED("CDOC_PREVIEW_NOT_SUPPORTED",
            "PDF 만 미리보기를 지원합니다."),
    CDOC_PREVIEW_FAILED("CDOC_PREVIEW_FAILED",
            "미리보기 생성에 실패했습니다.");

    private final String code;
    private final String message;
}
