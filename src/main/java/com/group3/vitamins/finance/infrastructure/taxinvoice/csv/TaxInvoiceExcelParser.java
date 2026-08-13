package com.group3.vitamins.finance.infrastructure.taxinvoice.csv;

import com.group3.vitamins.finance.domain.exception.FinanceErrorCode;
import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 세금계산서를 엑셀(.xlsx/.xls)로 내보낸 파일을 헤더+행(Map) 구조로 읽는다. cash_flow의
 * CashFlowExcelParser와 완전히 동일한 사고방식(헤더 행 판정·셀 값 정규화·비밀번호 처리)을 그대로
 * 옮긴 것으로, 다른 도메인 코드를 참조·공유하지 않는 finance 전용 독립 클래스다.
 *
 * <p>여러 시트가 있는 파일도 첫 번째 시트만 읽는다(cash_flow와 동일 — 시트 자동 판정은 지원하지 않는다,
 * 2026-08-12 확정. 필요하면 엑셀에서 원하는 시트를 활성 시트로 두고 저장해서 올려야 한다).
 */
@Slf4j
@Component
public class TaxInvoiceExcelParser {

    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DataFormatter dataFormatter = new DataFormatter();

    public TaxInvoiceCsvTable parse(byte[] fileBytes, String password) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new NotFoundException(FinanceErrorCode.FINANCE_INVALID_CSV_FILE);
        }

        try (Workbook workbook = openWorkbook(fileBytes, password)) {
            Sheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();
            if (lastRowNum < 0) {
                throw new NotFoundException(FinanceErrorCode.FINANCE_INVALID_CSV_FILE);
            }

            int headerRowIndex = findHeaderRowIndex(sheet, lastRowNum);
            Row headerRow = sheet.getRow(headerRowIndex);
            HeaderColumns headerColumns = readHeaders(headerRow);
            if (headerColumns.headers().isEmpty()) {
                throw new NotFoundException(FinanceErrorCode.FINANCE_INVALID_CSV_FILE);
            }

            List<Map<String, String>> rows = new ArrayList<>();
            for (int r = headerRowIndex + 1; r <= lastRowNum; r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                Map<String, String> parsedRow = readRow(row, headerColumns);
                if (parsedRow.values().stream().allMatch(java.util.Objects::isNull)) {
                    continue; // 완전히 빈 행(합계 구분용 빈 줄 등)은 건너뛴다
                }
                rows.add(parsedRow);
            }

            return new TaxInvoiceCsvTable(headerColumns.headers(), rows, extractTitleText(sheet, headerRowIndex));
        } catch (DomainException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            log.warn("세금계산서 엑셀 파싱 실패 - 유효하지 않은 파일로 변환", e);
            throw new NotFoundException(FinanceErrorCode.FINANCE_INVALID_CSV_FILE, e);
        }
    }

    /** 헤더 위 제목 줄(들)의 실제 값을 공백으로 이어붙인다 — CSV의 extractTitleText와 동일 목적. */
    private String extractTitleText(Sheet sheet, int headerRowIndex) {
        StringBuilder titleText = new StringBuilder();
        for (int r = 0; r < headerRowIndex; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            int lastCellNum = Math.max(row.getLastCellNum(), 0);
            for (int c = 0; c < lastCellNum; c++) {
                Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                String value = cell == null ? null : cellValue(cell);
                if (value != null) {
                    titleText.append(value).append(' ');
                }
            }
        }
        return titleText.isEmpty() ? null : titleText.toString().trim();
    }

    private Workbook openWorkbook(byte[] fileBytes, String password) {
        boolean hasPassword = StringUtils.hasText(password);
        try {
            return hasPassword
                    ? WorkbookFactory.create(new ByteArrayInputStream(fileBytes), password)
                    : WorkbookFactory.create(new ByteArrayInputStream(fileBytes));
        } catch (EncryptedDocumentException e) {
            log.warn("세금계산서 엑셀 파싱 실패 - 비밀번호 {} - 원인: {}",
                    hasPassword ? "틀림" : "필요", e.getMessage());
            throw new ValidationException(hasPassword
                    ? FinanceErrorCode.FINANCE_CSV_PASSWORD_INVALID
                    : FinanceErrorCode.FINANCE_CSV_PASSWORD_REQUIRED);
        } catch (IOException e) {
            throw new NotFoundException(FinanceErrorCode.FINANCE_INVALID_CSV_FILE, e);
        }
    }

    private int findHeaderRowIndex(Sheet sheet, int lastRowNum) {
        int maxFilledCount = 0;
        int headerIndex = 0;
        for (int r = 0; r <= lastRowNum; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            int filledCount = countFilledCells(row);
            if (filledCount > maxFilledCount) {
                maxFilledCount = filledCount;
                headerIndex = r;
            }
        }
        return headerIndex;
    }

    private int countFilledCells(Row row) {
        int lastCellNum = Math.max(row.getLastCellNum(), 0);
        int count = 0;
        for (int c = 0; c < lastCellNum; c++) {
            Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell != null && cellValue(cell) != null) {
                count++;
            }
        }
        return count;
    }

    private record HeaderColumns(List<String> headers, int startColumn) {
    }

    private HeaderColumns readHeaders(Row headerRow) {
        int lastCellNum = Math.max(headerRow.getLastCellNum(), 0);
        List<String> raw = new ArrayList<>();
        int firstNonBlank = -1;
        int lastNonBlank = -1;
        for (int c = 0; c < lastCellNum; c++) {
            Cell cell = headerRow.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            String value = cell == null ? null : cellValue(cell);
            raw.add(value);
            if (value != null) {
                if (firstNonBlank == -1) {
                    firstNonBlank = c;
                }
                lastNonBlank = c;
            }
        }
        if (firstNonBlank == -1) {
            return new HeaderColumns(List.of(), 0);
        }

        List<String> headers = new ArrayList<>();
        for (int c = firstNonBlank; c <= lastNonBlank; c++) {
            String value = raw.get(c);
            headers.add(value == null ? ("컬럼" + (c + 1)) : value);
        }
        return new HeaderColumns(disambiguateHeaders(headers), firstNonBlank);
    }

    /**
     * 같은 이름 헤더가 여러 번 나오면(공급자/공급받는자 블록이 "상호"/"대표자명"/"종사업장번호"를 각자 갖고
     * 있어서 이름이 겹친다) 두 번째부터 " (2)", " (3)"... 을 붙여 구분한다 — CSV의 disambiguateHeaders와
     * 동일 목적(2026-08-13, 실제 파일로 발견).
     */
    private List<String> disambiguateHeaders(List<String> rawHeaders) {
        Map<String, Integer> occurrenceCount = new java.util.HashMap<>();
        List<String> result = new ArrayList<>();
        for (String header : rawHeaders) {
            int occurrence = occurrenceCount.merge(header, 1, Integer::sum);
            result.add(occurrence == 1 ? header : header + " (" + occurrence + ")");
        }
        return result;
    }

    private Map<String, String> readRow(Row row, HeaderColumns headerColumns) {
        Map<String, String> parsed = new LinkedHashMap<>();
        List<String> headers = headerColumns.headers();
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = row.getCell(headerColumns.startColumn() + i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            parsed.put(headers.get(i), cell == null ? null : cellValue(cell));
        }
        return parsed;
    }

    private String cellValue(Cell cell) {
        String value = switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case NUMERIC -> numeric(cell);
            case FORMULA -> dataFormatter.formatCellValue(cell);
            default -> null;
        };
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String numeric(Cell cell) {
        if (DateUtil.isCellDateFormatted(cell)) {
            LocalDateTime dateTime = cell.getLocalDateTimeCellValue();
            return dateTime.toLocalTime().equals(java.time.LocalTime.MIDNIGHT)
                    ? dateTime.toLocalDate().format(DATE_ONLY)
                    : dateTime.format(DATE_TIME);
        }
        return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
    }
}
