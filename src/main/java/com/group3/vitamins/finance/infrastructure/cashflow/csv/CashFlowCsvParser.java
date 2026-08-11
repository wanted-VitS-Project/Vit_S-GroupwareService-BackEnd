package com.group3.vitamins.finance.infrastructure.cashflow.csv;

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
 * 은행 CSV를 헤더+행(Map) 구조로 파싱한다. 따옴표로 감싼 필드 안의 콤마·개행은 commons-csv가 처리한다.
 *
 * <p>⚠️ 첫 줄이 곧 헤더라고 가정하지 않는다 — 실제 은행 CSV는 계좌번호·조회기간·총건수 같은 안내 줄이
 * 진짜 표 앞에 몇 줄 붙는 경우가 있다(2026-08-10, 실제 파일로 확인). 전체 행의 "컬럼 개수" **최댓값**을
 * 진짜 표의 폭으로 보고, 그 폭과 처음 일치하는 행을 헤더로 판정한다 — 헤더는 모든 컬럼에 이름이 채워져
 * 있어 항상 표에서 가장 넓은 줄이라는 성질을 이용한다. 안내 줄이 아예 없는 파일(헤더가 1번째 줄)도
 * 그대로 맞아떨어진다.
 */
@Component
public class CashFlowCsvParser {

    public CashFlowCsvTable parse(byte[] fileBytes) {
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

            return new CashFlowCsvTable(headers, rows);
        } catch (IOException | IllegalArgumentException e) {
            throw new NotFoundException(FinanceErrorCode.FINANCE_INVALID_CSV_FILE, e);
        }
    }

    /**
     * 컬럼 개수 **최댓값**과 처음 일치하는 행의 인덱스를 헤더로 판정한다.
     * ⚠️ 최빈값이 아니라 최댓값이다 (2026-08-10, 실제 파일로 수정) — 헤더는 모든 컬럼에 이름이 채워져
     * 있어 항상 표에서 가장 넓은 줄인데, 데이터 행 중 일부 컬럼(예: "메모")이 자주 비어서 뒤쪽 콤마가
     * 잘려나가는 CSV라면 최빈값 기준으로는 오히려 데이터 행 쪽 폭이 더 흔해져 헤더보다 좁은 데이터
     * 행이 헤더로 잘못 뽑힌다. 최댓값 기준이면 안내 줄(폭이 좁음)도, 군데군데 비는 데이터 행도
     * 문제되지 않는다.
     */
    private int findHeaderRowIndex(List<CSVRecord> records) {
        int maxWidth = 0;
        for (CSVRecord record : records) {
            maxWidth = Math.max(maxWidth, record.size());
        }

        for (int i = 0; i < records.size(); i++) {
            if (records.get(i).size() == maxWidth) {
                return i;
            }
        }
        return 0;
    }

    /**
     * 국내 은행 CSV 내보내기는 EUC-KR(엑셀 기본 저장 인코딩)인 경우가 흔하다. BOM이 있으면 UTF-8,
     * 없으면 EUC-KR로 가정한다 — 완벽한 감지는 아니라 실제 은행 CSV 샘플로 나중에 검증이 필요하다.
     */
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

    /**
     * ⚠️ BOM이 있으면 UTF-8로 판정만 하고 실제로 건너뛰지는 않아서, 그 3바이트가 그대로 디코딩돼
     * 첫 헤더 앞에 U+FEFF가 붙어버렸다(2026-08-11, CodeRabbit 지적 — "﻿거래일시"처럼 헤더가
     * 오염돼 컬럼 매핑이 깨짐). BOM 길이만큼 스트림 시작 위치를 건너뛰도록 고쳤다.
     */
    private int bomLength(byte[] fileBytes) {
        return hasUtf8Bom(fileBytes) ? 3 : 0;
    }

    private boolean hasUtf8Bom(byte[] fileBytes) {
        return fileBytes.length >= 3
                && (fileBytes[0] & 0xFF) == 0xEF && (fileBytes[1] & 0xFF) == 0xBB && (fileBytes[2] & 0xFF) == 0xBF;
    }
}
