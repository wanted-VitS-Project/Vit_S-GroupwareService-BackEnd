package com.group3.vitamins.vitamate.fileindex.presentation.internal.dto.response;

import com.group3.vitamins.vitamate.fileindex.application.result.VitamateFileIndexSourceResult;
import io.swagger.v3.oas.annotations.media.Schema;

// Python worker가 파일을 다운로드하고 텍스트 추출할 때 사용할 응답
@Schema(description = "Python worker가 파일을 다운로드하고 텍스트 추출할 때 사용할 응답")
public record VitamateFileIndexSourceResponse(
        @Schema(description = "파일 버전 ID", example = "101")
        Long fileVersionId,
        @Schema(description = "파일 ID", example = "31")
        Long fileId,
        @Schema(description = "프로젝트 ID", example = "7")
        Long projectId,
        @Schema(description = "원본 파일명", example = "스마트시티_제안요청서.pdf")
        String originalFileName,
        @Schema(description = "확장자", example = "pdf")
        String extension,
        @Schema(description = "MIME 타입", example = "application/pdf")
        String mimeType,
        @Schema(description = "파일 크기(byte)", example = "1024000")
        Long sizeBytes,
        @Schema(description = "저장소 객체 키. 로그와 외부 응답에는 남기지 않는다.", example = "local/vitamate-test/rfp.pdf")
        String storageKey,
        @Schema(description = "Python worker 전용 다운로드 URL")
        String downloadUrl
) {
    public static VitamateFileIndexSourceResponse from(VitamateFileIndexSourceResult result) {
        return new VitamateFileIndexSourceResponse(
                result.fileVersionId(),
                result.fileId(),
                result.projectId(),
                result.originalFileName(),
                result.extension(),
                result.mimeType(),
                result.sizeBytes(),
                result.storageKey(),
                result.downloadUrl()
        );
    }
}
