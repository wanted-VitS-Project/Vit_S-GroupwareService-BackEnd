package com.group3.vitamins.employee.infrastructure;

import com.group3.vitamins.employee.application.result.ParsedEmployeeRow;
import com.group3.vitamins.employee.application.support.EmployeeBulkColumns;
import com.group3.vitamins.employee.domain.exception.EmployeeErrorCode;
import com.group3.vitamins.employee.infrastructure.adapter.PoiEmployeeExcelParserAdapter;
import com.group3.vitamins.employee.infrastructure.adapter.PoiEmployeeExcelTemplateAdapter;
import com.group3.vitamins.global.domain.common.error.DomainException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("POI 엑셀 어댑터 (템플릿 생성 · 파싱)")
class PoiEmployeeExcelAdapterTest {

    private final PoiEmployeeExcelTemplateAdapter templateAdapter = new PoiEmployeeExcelTemplateAdapter();
    private final PoiEmployeeExcelParserAdapter parserAdapter = new PoiEmployeeExcelParserAdapter();

    @Test
    @DisplayName("생성한 템플릿은 헤더만 있어 데이터 행이 0이다")
    void templateHasHeaderOnly() {
        byte[] template = templateAdapter.generate();
        assertThat(parserAdapter.parse(template)).isEmpty();
    }

    @Test
    @DisplayName("숫자 사번은 정수 문자열로, 날짜 셀은 yyyy-MM-dd 로 읽는다")
    void readsNumericAndDateCells() throws IOException {
        byte[] file = workbook(sheet -> {
            Row data = sheet.createRow(1);
            data.createCell(EmployeeBulkColumns.USER_ID).setCellValue(12345);      // 숫자 사번
            data.createCell(EmployeeBulkColumns.NAME).setCellValue("홍길동");
            data.createCell(EmployeeBulkColumns.DEPARTMENT).setCellValue("개발팀");
            data.createCell(EmployeeBulkColumns.JOB_POSITION).setCellValue("대리");
            dateCell(sheet.getWorkbook(), data, EmployeeBulkColumns.HIRED_AT, LocalDate.of(2026, 1, 3));
            data.createCell(EmployeeBulkColumns.EMAIL).setCellValue("a@b.com");
            data.createCell(EmployeeBulkColumns.PHONE).setCellValue("010-1111-2222");
            data.createCell(EmployeeBulkColumns.ROLE).setCellValue("MEMBER");
        });

        List<ParsedEmployeeRow> rows = parserAdapter.parse(file);

        assertThat(rows).hasSize(1);
        ParsedEmployeeRow r = rows.get(0);
        assertThat(r.rowNumber()).isEqualTo(2);       // 엑셀 행 번호 1-base
        assertThat(r.userId()).isEqualTo("12345");    // "12345.0" 아님
        assertThat(r.hiredAt()).isEqualTo("2026-01-03");
        assertThat(r.name()).isEqualTo("홍길동");
        assertThat(r.role()).isEqualTo("MEMBER");
    }

    @Test
    @DisplayName("완전히 빈 행은 건너뛴다")
    void skipsEmptyRows() throws IOException {
        byte[] file = workbook(sheet -> {
            sheet.createRow(1); // 빈 행
            Row data = sheet.createRow(2);
            data.createCell(EmployeeBulkColumns.USER_ID).setCellValue("EMP001");
            data.createCell(EmployeeBulkColumns.NAME).setCellValue("김");
        });

        List<ParsedEmployeeRow> rows = parserAdapter.parse(file);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).userId()).isEqualTo("EMP001");
    }

    @Test
    @DisplayName(".xls(HSSF) 바이너리도 동일하게 파싱한다")
    void readsXlsBinary() throws IOException {
        byte[] file;
        try (Workbook wb = new HSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("사원");
            Row header = sheet.createRow(0);
            for (int i = 0; i < EmployeeBulkColumns.HEADERS.size(); i++) {
                header.createCell(i).setCellValue(EmployeeBulkColumns.HEADERS.get(i));
            }
            Row data = sheet.createRow(1);
            data.createCell(EmployeeBulkColumns.USER_ID).setCellValue("EMP001");
            data.createCell(EmployeeBulkColumns.NAME).setCellValue("홍길동");
            data.createCell(EmployeeBulkColumns.ROLE).setCellValue("MEMBER");
            wb.write(out);
            file = out.toByteArray();
        }

        List<ParsedEmployeeRow> rows = parserAdapter.parse(file);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).userId()).isEqualTo("EMP001");
        assertThat(rows.get(0).name()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("엑셀이 아닌 바이너리는 EMP_FILE_TYPE_INVALID 로 던진다")
    void corruptFile() {
        assertThatThrownBy(() -> parserAdapter.parse("이건 엑셀이 아니다".getBytes()))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(EmployeeErrorCode.EMP_FILE_TYPE_INVALID);
    }

    // ---- 헬퍼 ---------------------------------------------------------------

    private interface SheetBuilder {
        void build(Sheet sheet);
    }

    private byte[] workbook(SheetBuilder builder) throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("사원");
            Row header = sheet.createRow(0);
            for (int i = 0; i < EmployeeBulkColumns.HEADERS.size(); i++) {
                header.createCell(i).setCellValue(EmployeeBulkColumns.HEADERS.get(i));
            }
            builder.build(sheet);
            wb.write(out);
            return out.toByteArray();
        }
    }

    private void dateCell(Workbook wb, Row row, int column, LocalDate date) {
        CreationHelper helper = wb.getCreationHelper();
        CellStyle style = wb.createCellStyle();
        style.setDataFormat(helper.createDataFormat().getFormat("yyyy-mm-dd"));
        Cell cell = row.createCell(column);
        cell.setCellValue(date);
        cell.setCellStyle(style);
    }
}
