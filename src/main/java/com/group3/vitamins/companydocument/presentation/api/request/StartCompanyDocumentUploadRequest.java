package com.group3.vitamins.companydocument.presentation.api.request;

import com.group3.vitamins.companydocument.application.command.StartCompanyDocumentUploadCommand;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 사내 문서 업로드 시작 요청(§1).
 *
 * <p>{@code companyDocumentId} 를 주면 그 문서의 새 버전, 없으면 새 문서(이때 {@code category} 필수)다.
 */
@Schema(description = "사내 문서 업로드 시작 요청")
public record StartCompanyDocumentUploadRequest(
        @Schema(description = "카테고리 enum(새 문서일 때 필수). FINANCE·COMPANY_INTRO·PERFORMANCE·CERTIFICATE·ETC",
                example = "FINANCE")
        String category,

        @Schema(description = "원본 파일명(확장자 포함)", example = "2026_재무제표.pdf")
        String originalFileName,

        @Schema(description = "파일 크기(bytes). 50MB 이하", example = "1048576")
        long sizeBytes,

        @Schema(description = "MIME 타입", example = "application/pdf", nullable = true)
        String mimeType,

        @Schema(description = "문서 표시명. 생략하면 확장자를 뗀 원본 파일명", example = "2026년 재무제표", nullable = true)
        String name,

        @Schema(description = "새 버전을 올릴 대상 문서. 생략하면 새 문서", example = "12", nullable = true)
        Long companyDocumentId,

        @Schema(description = "버전 코멘트", example = "1분기 갱신", nullable = true)
        String comment
) {

    public StartCompanyDocumentUploadCommand toCommand(String requesterUserId, String role) {
        return new StartCompanyDocumentUploadCommand(
                category, originalFileName, sizeBytes, mimeType, name, companyDocumentId, comment,
                requesterUserId, role);
    }
}
