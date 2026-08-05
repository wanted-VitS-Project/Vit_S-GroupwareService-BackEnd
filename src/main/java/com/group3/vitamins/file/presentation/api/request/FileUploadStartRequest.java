package com.group3.vitamins.file.presentation.api.request;

import com.group3.vitamins.file.application.command.StartFileUploadCommand;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "업로드 시작 요청. fileId 를 주면 그 문서의 새 버전, 없으면 새 문서(v1).")
public record FileUploadStartRequest(
        @Schema(description = "파일을 붙일 블록", example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
        Long blockId,

        @Schema(description = "원본 파일명(확장자 포함)", example = "제안서_v2.pdf",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String originalFileName,

        @Schema(description = "파일 크기(byte). 50MB 이하", example = "5242880",
                requiredMode = Schema.RequiredMode.REQUIRED)
        long sizeBytes,

        @Schema(description = "MIME 타입", example = "application/pdf")
        String mimeType,

        @Schema(description = "문서 표시명. 생략하면 확장자를 뗀 원본 파일명", example = "제안서")
        String name,

        @Schema(description = "새 버전을 올릴 대상 문서. 생략하면 새 문서", example = "31")
        Long fileId,

        @Schema(description = "버전 코멘트", example = "최종본 반영")
        String comment,

        @Schema(description = "동명 문서가 있어도 진행할지. 기본 false", example = "false")
        Boolean allowDuplicateName
) {

    public StartFileUploadCommand toCommand(String requesterUserId, String role) {
        return new StartFileUploadCommand(
                blockId,
                originalFileName,
                sizeBytes,
                mimeType,
                name,
                fileId,
                comment,
                allowDuplicateName != null && allowDuplicateName,
                requesterUserId,
                role
        );
    }
}
