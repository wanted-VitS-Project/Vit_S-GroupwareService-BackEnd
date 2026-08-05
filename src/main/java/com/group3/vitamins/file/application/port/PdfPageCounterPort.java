package com.group3.vitamins.file.application.port;

import java.util.Optional;

/**
 * PDF 페이지 수 추출 아웃바운드 포트(§2 · VER-008). 실패해도 업로드는 성공 처리하고 페이지 수만 비운다.
 * 구현은 {@code infrastructure/storage/PdfBoxPageCounterAdapter} (PDFBox).
 */
public interface PdfPageCounterPort {

    /** PDF 바이트에서 총 페이지 수를 센다. 파싱 실패 시 empty. */
    Optional<Integer> countPages(byte[] pdfBytes);
}
