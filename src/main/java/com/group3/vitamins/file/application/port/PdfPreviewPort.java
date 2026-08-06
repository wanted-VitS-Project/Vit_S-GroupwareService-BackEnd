package com.group3.vitamins.file.application.port;

/**
 * PDF 미리보기 생성 아웃바운드 포트(§10). 앞 N페이지만 남긴 PDF 를 만든다 — presigned 를 주면
 * 전체 PDF 에 접근돼 "최대 5페이지" 제한이 무의미해지므로 서버가 잘라서 반환한다.
 * 구현은 {@code infrastructure/storage/PdfBoxPreviewAdapter}.
 */
public interface PdfPreviewPort {

    /** 앞 maxPages 페이지만 남긴 PDF 를 만든다. 파싱·저장 실패 시 예외를 던진다(서비스가 500 으로 변환). */
    Preview render(byte[] pdfBytes, int maxPages);

    /** 잘라낸 PDF 바이트 + 잘라낸 페이지 수 + 원본 전체 페이지 수. */
    record Preview(byte[] content, int previewPageCount, int totalPageCount) {
    }
}
