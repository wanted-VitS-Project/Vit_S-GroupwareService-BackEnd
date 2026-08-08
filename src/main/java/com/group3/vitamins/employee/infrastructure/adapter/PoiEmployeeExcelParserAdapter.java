package com.group3.vitamins.employee.infrastructure.adapter;

import com.group3.vitamins.employee.application.port.EmployeeExcelParserPort;
import com.group3.vitamins.employee.application.result.ParsedEmployeeRow;
import com.group3.vitamins.employee.application.support.EmployeeBulkColumns;
import com.group3.vitamins.employee.domain.exception.EmployeeErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link EmployeeExcelParserPort} 구현 — POI {@code WorkbookFactory} 로 .xlsx·.xls 를 모두 읽는다 (employee.md §7·§8).
 * 첫 시트만 사용하고, 헤더(0행) 다음부터 데이터로 본다. 완전히 빈 행은 건너뛴다.
 */
@Component
public class PoiEmployeeExcelParserAdapter implements EmployeeExcelParserPort {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    // 셀 문자열 변환기 — 서식 그대로("12,345" 등)가 아니라 값만 필요하므로 숫자·날짜는 직접 처리하고 나머지에만 쓴다.
    private final DataFormatter dataFormatter = new DataFormatter();

    @Override
    public List<ParsedEmployeeRow> parse(byte[] content) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheetAt(0);
            List<ParsedEmployeeRow> rows = new ArrayList<>();

            int lastRow = sheet.getLastRowNum();
            for (int r = EmployeeBulkColumns.FIRST_DATA_ROW; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                ParsedEmployeeRow parsed = readRow(row);
                if (isEmpty(parsed)) {
                    continue; // 중간의 빈 행은 데이터로 세지 않는다
                }
                rows.add(parsed);
            }
            return rows;
        } catch (IOException | RuntimeException e) {
            // 확장자는 맞지만 내용이 엑셀이 아니거나 손상된 경우 — 파일 형식 오류(400)로 변환한다(파일 열기 실패).
            throw new ValidationException(EmployeeErrorCode.EMP_FILE_TYPE_INVALID, e);
        }
    }

    private ParsedEmployeeRow readRow(Row row) {
        return new ParsedEmployeeRow(
                row.getRowNum() + 1, // 사람이 보는 엑셀 행 번호(1-base)
                cell(row, EmployeeBulkColumns.USER_ID),
                cell(row, EmployeeBulkColumns.NAME),
                cell(row, EmployeeBulkColumns.DEPARTMENT),
                cell(row, EmployeeBulkColumns.JOB_POSITION),
                cell(row, EmployeeBulkColumns.HIRED_AT),
                cell(row, EmployeeBulkColumns.EMAIL),
                cell(row, EmployeeBulkColumns.PHONE),
                cell(row, EmployeeBulkColumns.ROLE));
    }

    /** 셀을 문자열로 읽어 trim 한다. 빈 값은 null. 숫자 사번은 정수 문자열로, 엑셀 날짜는 yyyy-MM-dd 로 정규화한다. */
    private String cell(Row row, int column) {
        Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return null;
        }
        String value = switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case NUMERIC -> numeric(cell);
            case FORMULA -> dataFormatter.formatCellValue(cell); // 계산식은 캐시된 표시값을 쓴다
            default -> null; // BLANK · _NONE · ERROR
        };
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** 숫자 셀 — 날짜 서식이면 yyyy-MM-dd, 아니면 정수는 소수점 없이("12345"), 소수는 그대로. 과학표기·"12345.0" 오염을 막는다. */
    private String numeric(Cell cell) {
        if (DateUtil.isCellDateFormatted(cell)) {
            LocalDate date = cell.getLocalDateTimeCellValue().toLocalDate();
            return date.format(ISO_DATE);
        }
        // BigDecimal 로 훑어 불필요한 0 을 없앤다 — double toString 의 "1.2345E4"·".0" 문제를 피한다.
        return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
    }

    private boolean isEmpty(ParsedEmployeeRow r) {
        return r.userId() == null && r.name() == null && r.department() == null
                && r.jobPosition() == null && r.hiredAt() == null && r.email() == null
                && r.phone() == null && r.role() == null;
    }
}
