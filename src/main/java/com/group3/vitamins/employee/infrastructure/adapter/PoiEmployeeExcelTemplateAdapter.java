package com.group3.vitamins.employee.infrastructure.adapter;

import com.group3.vitamins.employee.application.port.EmployeeExcelTemplatePort;
import com.group3.vitamins.employee.application.support.EmployeeBulkColumns;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/** {@link EmployeeExcelTemplatePort} 구현 — POI 로 헤더만 있는 .xlsx 를 만든다 (employee.md §6). */
@Component
public class PoiEmployeeExcelTemplateAdapter implements EmployeeExcelTemplatePort {

    private static final int COLUMN_WIDTH = 4000; // 1/256 문자폭 단위

    @Override
    public byte[] generate() {
        // try-with-resources 로 Workbook·스트림을 닫는다. XSSFWorkbook 은 메모리에 시트를 쌓았다가 write 로 직렬화한다.
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("사원");
            CellStyle headerStyle = boldStyle(workbook);

            Row header = sheet.createRow(0);
            List<String> headers = EmployeeBulkColumns.HEADERS;
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, COLUMN_WIDTH);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            // 메모리 스트림이라 실제로는 나지 않지만, 검사 예외를 도메인으로 흘리지 않게 언체크로 감싼다.
            throw new UncheckedIOException("사원 일괄 등록 템플릿 생성 실패", e);
        }
    }

    private CellStyle boldStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
}
