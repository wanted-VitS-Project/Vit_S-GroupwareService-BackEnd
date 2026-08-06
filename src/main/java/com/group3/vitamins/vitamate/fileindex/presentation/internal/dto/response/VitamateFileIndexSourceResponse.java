package com.group3.vitamins.vitamate.fileindex.presentation.internal.dto.response;

import com.group3.vitamins.vitamate.fileindex.application.result.VitamateFileIndexSourceResult;

// Python worker가 파일을 다운로드하고 텍스트 추출할 때 사용할 응답
public record VitamateFileIndexSourceResponse(
        Long fileVersionId,
        Long fileId,
        Long projectId,
        String originalFileName,
        String extension,
        String mimeType,
        Long sizeBytes,
        String storageKey,
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