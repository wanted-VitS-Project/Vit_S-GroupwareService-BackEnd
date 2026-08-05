package com.group3.vitamins.approval.presentation.api.response;

import com.group3.vitamins.approval.application.result.ApprovalDocumentView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record AddApprovalDocumentResponse(
        @Schema(description = "결재 문서 구분 번호", example = "1")
        Long documentId,

        @Schema(description = "파일 버전 구분 번호", example = "10")
        Long fileVersionId,

        @Schema(description = "파일명(라이브 조회)", example = "제안서_v1.pdf")
        String fileName,

        @Schema(description = "파일 크기(byte, 라이브 조회)", example = "4404019")
        Long fileSize,

        @Schema(description = "업로드 완료 일시(라이브 조회)", example = "2026-08-04T13:00:00")
        LocalDateTime uploadedAt
) {

    public static AddApprovalDocumentResponse from(ApprovalDocumentView view) {
        return new AddApprovalDocumentResponse(
                view.documentId(), view.fileVersionId(), view.fileName(), view.fileSize(), view.uploadedAt());
    }
}
