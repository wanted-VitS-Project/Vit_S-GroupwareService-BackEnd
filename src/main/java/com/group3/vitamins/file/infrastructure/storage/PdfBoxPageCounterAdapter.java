package com.group3.vitamins.file.infrastructure.storage;

import com.group3.vitamins.file.application.port.PdfPageCounterPort;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** PDFBox 로 PDF 페이지 수를 센다. 손상·암호화 등 파싱 실패는 empty 로 흡수한다(업로드는 성공 처리). */
@Component
@Slf4j
public class PdfBoxPageCounterAdapter implements PdfPageCounterPort {

    @Override
    public Optional<Integer> countPages(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            return Optional.of(document.getNumberOfPages());
        } catch (Exception e) {
            log.warn("PDF 페이지 수 추출 실패 - 페이지 수 없이 업로드 완료 처리", e);
            return Optional.empty();
        }
    }
}
