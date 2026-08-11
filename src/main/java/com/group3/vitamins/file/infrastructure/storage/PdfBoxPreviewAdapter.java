package com.group3.vitamins.file.infrastructure.storage;

import com.group3.vitamins.file.application.port.PdfPreviewPort;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

/** PDFBox 로 앞 N페이지만 남긴 PDF 를 생성한다(§10). 실패는 unchecked 로 던져 서비스가 500 으로 변환한다. */
@Component
public class PdfBoxPreviewAdapter implements PdfPreviewPort {

    @Override
    public Preview render(byte[] pdfBytes, int maxPages) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            int total = document.getNumberOfPages();
            int keep = Math.min(maxPages, total);

            // 뒤에서부터 제거해 앞 keep 페이지만 남긴다.
            while (document.getNumberOfPages() > keep) {
                document.removePage(document.getNumberOfPages() - 1);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return new Preview(out.toByteArray(), keep, total);
        } catch (Exception e) {
            throw new IllegalStateException("PDF 미리보기 생성 실패", e);
        }
    }
}
