package com.group3.vitamins.companydocument.application.result;

/** 사내 문서 미리보기(§9) 결과 — 앞 5페이지만 남긴 PDF 바이너리 + 페이지 수. */
public record CompanyDocumentPreviewResult(
        byte[] content,
        int previewPageCount,
        int totalPageCount
) {
}
