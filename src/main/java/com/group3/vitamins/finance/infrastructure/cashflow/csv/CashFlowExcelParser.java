package com.group3.vitamins.finance.infrastructure.cashflow.csv;

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
 * 은행이 CSV 대신 엑셀(.xlsx/.xls)로 내보낸 파일을 헤더+행(Map) 구조로 읽는다.
 * {@link CashFlowCsvParser}와 같은 {@link CashFlowCsvTable} 결과를 만들어 이후 파이프라인(추천·행변환)이
 * 원본 형식과 무관하게 동작한다. 셀 처리 방식(날짜 서식 감지, 숫자 "12345.0" 오염 방지)은 Apache POI의
 * 표준적인 사용법을 따른 것으로, 다른 도메인 코드를 참조·공유하지 않는 finance 전용 독립 클래스다.
 *
 * <p>⚠️ 1번째 행이 곧 헤더라고 가정하지 않는다 — {@link CashFlowCsvParser}와 동일한 이유(계좌번호·조회기간
 * 같은 안내 줄이 표 앞에 붙는 실제 은행 파일)로, **실제 값이 채워진 칸의 개수**가 가장 많은 행을 헤더로
 * 판정한다(칸이 스타일 때문에 넓게 잡혀도 값 없는 칸은 안 센다 — {@link #findHeaderRowIndex} 참고).
 */
@Slf4j
@Component
public class CashFlowExcelParser {

    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter TIME_ONLY = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final DataFormatter dataFormatter = new DataFormatter();

    public CashFlowCsvTable parse(byte[] fileBytes, String password) {
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

            return new CashFlowCsvTable(headerColumns.headers(), rows);
        } catch (DomainException e) {
            // openWorkbook이 이미 FINANCE_CSV_PASSWORD_REQUIRED/INVALID 등 정확한 도메인 코드로 던진
            // 예외다 — 아래 포괄 catch가 이걸 "유효하지 않은 형식"으로 덮어쓰지 않도록 먼저 그대로 전파한다.
            throw e;
        } catch (IOException | RuntimeException e) {
            // 확장자는 xlsx/xls 지만 내용이 손상됐거나 엑셀이 아닌 경우 — 유효하지 않은 파일로 변환한다.
            log.warn("입출금 내역 엑셀 파싱 실패 - 유효하지 않은 파일로 변환", e);
            throw new NotFoundException(FinanceErrorCode.FINANCE_INVALID_CSV_FILE, e);
        }
    }

    /**
     * 비밀번호가 왔으면 그걸로 열어보고, 실패 이유가 "이 파일은 애초에 암호가 걸려있다"인지
     * "비밀번호가 틀렸다"인지 — 호출자가 비밀번호를 보냈는지 여부로 구분한다(POI 예외 메시지 문자열에
     * 의존하지 않는다). 프론트가 이 둘을 구분해야 "비밀번호 입력 모달을 새로 띄울지"
     * "같은 모달에서 재입력시킬지"를 판단할 수 있다.
     */
    private Workbook openWorkbook(byte[] fileBytes, String password) {
        boolean hasPassword = StringUtils.hasText(password);
        try {
            return hasPassword
                    ? WorkbookFactory.create(new ByteArrayInputStream(fileBytes), password)
                    : WorkbookFactory.create(new ByteArrayInputStream(fileBytes));
        } catch (EncryptedDocumentException e) {
            log.warn("입출금 내역 엑셀 파싱 실패 - 비밀번호 {} - 원인: {}",
                    hasPassword ? "틀림" : "필요", e.getMessage());
            throw new ValidationException(hasPassword
                    ? FinanceErrorCode.FINANCE_CSV_PASSWORD_INVALID
                    : FinanceErrorCode.FINANCE_CSV_PASSWORD_REQUIRED);
        } catch (IOException e) {
            throw new NotFoundException(FinanceErrorCode.FINANCE_INVALID_CSV_FILE, e);
        }
    }

    /**
     * "실제 값이 채워진 칸의 개수"가 가장 많은 행을 헤더로 판정한다.
     * ⚠️ 칸 존재 범위(POI {@code getLastCellNum}) 기준에서 두 번째로 정정했다 (2026-08-10, 실제 파일로
     * 발견) — 은행 리포트 템플릿은 "성명"/"계좌번호"/"조회기간" 같은 안내 줄에도 표 전체 너비만큼
     * 서식(스타일)을 미리 입혀두는 경우가 있다. 그러면 안내 줄의 실제 값은 3~4개뿐인데도
     * {@code getLastCellNum()}은 진짜 헤더와 똑같이(예: 9) 잡혀서, 서식만 넓고 내용은 빈 안내 줄이
     * 먼저 헤더로 뽑히는 문제가 있었다. 칸 "존재 범위"가 아니라 칸에 **실제 값이 들어있는 개수**로
     * 재면 안내 줄(값 3~4개)과 진짜 헤더(모든 칸에 이름이 있어 값 개수가 최대)가 구분된다.
     */
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

    /** 이 행에서 실제 값이 들어있는 칸의 개수 — 칸이 스타일만으로 넓게 잡혀있어도 값 없는 칸은 안 센다. */
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

    /** 헤더 목록과, 그 헤더가 실제 시작하는 칸 위치(앞쪽 빈 칸을 건너뛴 오프셋) — 데이터 행도 같은 위치로 읽어야 컬럼이 안 밀린다. */
    private record HeaderColumns(List<String> headers, int startColumn) {
    }

    /**
     * 헤더 행 — 처음으로 실제 값이 있는 칸부터 마지막으로 값이 있는 칸까지만 읽는다(앞뒤로 서식만
     * 있고 내용은 없는 칸은 버린다, 2026-08-10 — 앞쪽에 진짜 빈 칸이 있는 실제 파일로 발견). 중간에
     * 빈 칸이 있으면(드묾) 자리 표시자로 채운다.
     */
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
        return new HeaderColumns(headers, firstNonBlank);
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
            default -> null; // BLANK · _NONE · ERROR
        };
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 날짜 서식 셀은 세 갈래로 정규화한다 — 시각만 있으면 "HH:mm:ss", 시간이 자정이면 날짜만
     * "yyyy-MM-dd", 둘 다 있으면 "yyyy-MM-dd HH:mm:ss"(뒤 단계의 {@code CashFlowCsvRowParser}가 이
     * 포맷들을 시도 목록에 갖고 있다). 일반 숫자는 BigDecimal로 훑어 "12345.0"·과학표기 오염을 없앤다.
     *
     * <p>⚠️ **시각 전용 셀 처리는 값(시리얼)이 1 미만인지로 판정한다** (2026-08-13, 프론트 제보로 발견).
     * 엑셀은 "11:20:15"처럼 시각만 넣은 셀을 "0일차 + 시각"인 소수(0.472…)로 저장하고, 이걸 그대로
     * {@code getLocalDateTimeCellValue()}로 읽으면 기준일인 **1899-12-31이 날짜로 따라붙는다.**
     * 거래일자·거래시간이 분리된 은행 엑셀(SEPARATE)에서 미리보기에 "1899-12-31 11:20:15"가 보이고
     * 업로드는 "시간 형식을 해석할 수 없습니다"로 실패하던 원인이 이것이다. 하루 미만이면 날짜 성분이
     * 애초에 없는 값이므로 시각만 남긴다.
     */
    private String numeric(Cell cell) {
        if (DateUtil.isCellDateFormatted(cell)) {
            LocalDateTime dateTime = cell.getLocalDateTimeCellValue();
            if (cell.getNumericCellValue() < 1.0d) {
                return dateTime.toLocalTime().format(TIME_ONLY);
            }
            return dateTime.toLocalTime().equals(java.time.LocalTime.MIDNIGHT)
                    ? dateTime.toLocalDate().format(DATE_ONLY)
                    : dateTime.format(DATE_TIME);
        }
        return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
    }
}
