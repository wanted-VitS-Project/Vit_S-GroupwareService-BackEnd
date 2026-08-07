package com.group3.vitamins.vitamate.fileindex.application.result;

// Python worker가 파일 텍스트 추출에 사용할 파일 버전 정보
public record VitamateFileIndexSourceResult(
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
}