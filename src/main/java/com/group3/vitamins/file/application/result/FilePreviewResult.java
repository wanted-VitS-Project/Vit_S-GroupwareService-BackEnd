package com.group3.vitamins.file.application.result;

/** 미리보기(§10) 결과 — 잘라낸 PDF 바이트 + 페이지 수 헤더용 값. JSON 이 아니라 바이너리로 응답된다. */
public record FilePreviewResult(
        byte[] content,
        int previewPageCount,
        int totalPageCount
) {
}
