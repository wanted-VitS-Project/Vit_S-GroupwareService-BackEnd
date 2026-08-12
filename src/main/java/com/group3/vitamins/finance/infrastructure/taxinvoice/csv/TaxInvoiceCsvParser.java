package com.group3.vitamins.finance.infrastructure.taxinvoice.csv;

import com.group3.vitamins.finance.domain.exception.FinanceErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 세금계산서 CSV를 헤더+행(Map) 구조로 파싱한다. cash_flow의 CashFlowCsvParser와 완전히 동일한 로직
 * (헤더는 실제 값이 채워진 칸 개수가 가장 많은 행, 인코딩은 BOM 유무로 UTF-8/EUC-KR 추정).
 */
@Component
public class TaxInvoiceCsvParser {

    public TaxInvoiceCsvTable parse(byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new NotFoundException(FinanceErrorCode.FINANCE_INVALID_CSV_FILE);
        }

        try (Reader reader = new InputStreamReader(
                new java.io.ByteArrayInputStream(fileBytes, bomLength(fileBytes), fileBytes.length - bomLength(fileBytes)),
                resolveCharset(fileBytes));
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setTrim(true)
                     .setIgnoreEmptyLines(true)
                     .build()
                     .parse(reader)) {

            List<CSVRecord> allRecords = parser.getRecords();
            if (allRecords.isEmpty()) {
                throw new NotFoundException(FinanceErrorCode.FINANCE_INVALID_CSV_FILE);
            }

            int headerRowIndex = findHeaderRowIndex(allRecords);
            CSVRecord headerRecord = allRecords.get(headerRowIndex);
            List<String> headers = new ArrayList<>();
            headerRecord.forEach(headers::add);
            if (headers.isEmpty()) {
                throw new NotFoundException(FinanceErrorCode.FINANCE_INVALID_CSV_FILE);
            }

            List<Map<String, String>> rows = new ArrayList<>();
            for (int i = headerRowIndex + 1; i < allRecords.size(); i++) {
                CSVRecord record = allRecords.get(i);
                Map<String, String> row = new LinkedHashMap<>();
                for (int c = 0; c < headers.size(); c++) {
                    String value = c < record.size() ? record.get(c) : null;
                    row.put(headers.get(c), (value == null || value.isBlank()) ? null : value);
                }
                rows.add(row);
            }

            return new TaxInvoiceCsvTable(headers, rows);
        } catch (IOException | IllegalArgumentException e) {
            throw new NotFoundException(FinanceErrorCode.FINANCE_INVALID_CSV_FILE, e);
        }
    }

    /** "실제 값이 채워진 칸의 개수"가 가장 많은 행을 헤더로 판정한다 — cash_flow의 CashFlowCsvParser와 동일. */
    private int findHeaderRowIndex(List<CSVRecord> records) {
        int maxFilledCount = 0;
        int headerIndex = 0;
        for (int i = 0; i < records.size(); i++) {
            int filledCount = countFilledFields(records.get(i));
            if (filledCount > maxFilledCount) {
                maxFilledCount = filledCount;
                headerIndex = i;
            }
        }
        return headerIndex;
    }

    /** 이 행에서 실제 값이 들어있는 칸의 개수 — 콤마로 자리만 차지한 빈 칸은 안 센다. */
    private int countFilledFields(CSVRecord record) {
        int count = 0;
        for (int c = 0; c < record.size(); c++) {
            String value = record.get(c);
            if (value != null && !value.isBlank()) {
                count++;
            }
        }
        return count;
    }

    private java.nio.charset.Charset resolveCharset(byte[] fileBytes) {
        if (hasUtf8Bom(fileBytes)) {
            return StandardCharsets.UTF_8;
        }
        try {
            return java.nio.charset.Charset.forName("EUC-KR");
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }

    private int bomLength(byte[] fileBytes) {
        return hasUtf8Bom(fileBytes) ? 3 : 0;
    }

    private boolean hasUtf8Bom(byte[] fileBytes) {
        return fileBytes.length >= 3
                && (fileBytes[0] & 0xFF) == 0xEF && (fileBytes[1] & 0xFF) == 0xBB && (fileBytes[2] & 0xFF) == 0xBF;
    }
}
