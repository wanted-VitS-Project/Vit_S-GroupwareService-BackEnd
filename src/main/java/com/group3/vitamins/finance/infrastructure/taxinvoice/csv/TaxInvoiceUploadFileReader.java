package com.group3.vitamins.finance.infrastructure.taxinvoice.csv;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 확장자로 CSV/엑셀을 구분해 알맞은 파서로 넘긴다(cash_flow의 CashFlowUploadFileReader와 동일 방식).
 * 둘 다 같은 TaxInvoiceCsvTable을 만들어내므로 이후 파이프라인(추천·행변환)은 원본 파일 형식을 몰라도 된다.
 */
@Component
@RequiredArgsConstructor
public class TaxInvoiceUploadFileReader {

    private static final Set<String> EXCEL_EXTENSIONS = Set.of("xlsx", "xls");

    private final TaxInvoiceCsvParser csvParser;
    private final TaxInvoiceExcelParser excelParser;

    public TaxInvoiceCsvTable read(byte[] fileBytes, String originalFilename, String password) {
        if (EXCEL_EXTENSIONS.contains(extensionOf(originalFilename))) {
            return excelParser.parse(fileBytes, password);
        }
        // CSV는 애초에 암호화가 되는 포맷이 아니라 password는 그냥 무시한다.
        return csvParser.parse(fileBytes);
    }

    private String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase();
    }
}
