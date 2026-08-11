package com.group3.vitamins.finance.infrastructure.cashflow.csv;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 확장자로 CSV/엑셀을 구분해 알맞은 파서로 넘긴다. 둘 다 같은 {@link CashFlowCsvTable}을 만들어내므로
 * 이후 파이프라인(추천·행변환)은 원본 파일 형식을 몰라도 된다.
 */
@Component
@RequiredArgsConstructor
public class CashFlowUploadFileReader {

    private static final Set<String> EXCEL_EXTENSIONS = Set.of("xlsx", "xls");

    private final CashFlowCsvParser csvParser;
    private final CashFlowExcelParser excelParser;

    public CashFlowCsvTable read(byte[] fileBytes, String originalFilename, String password) {
        if (EXCEL_EXTENSIONS.contains(extensionOf(originalFilename))) {
            return excelParser.parse(fileBytes, password);
        }
        // CSV는 애초에 암호화가 되는 포맷이 아니라 password는 그냥 무시한다.
        // 확장자가 csv 이거나 판단할 수 없으면(확장자 없음 등) 기존처럼 CSV로 시도한다 — 실제로 CSV가
        // 아니면 CashFlowCsvParser가 파싱 실패 시점에 FINANCE_INVALID_CSV_FILE로 던진다.
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
