package com.group3.vitamins.vitamate.filecleanup.application.result;

// 비타메이트 파생 데이터 정리 결과
public record CleanupVitamateFileDerivedDataResult(
        Long fileId,
        int deletedCitationCount,
        int deletedAnalysisDocumentCount,
        int deletedDocumentChunkCount,
        int deletedFileIndexCount
) {
}