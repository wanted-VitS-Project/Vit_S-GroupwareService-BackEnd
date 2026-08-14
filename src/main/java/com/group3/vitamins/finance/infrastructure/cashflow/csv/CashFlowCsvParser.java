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
 * <p>⚠️ 첫 줄이 곧 헤더라고 가정하지 않는다 — 실제 은행 CSV는 계좌번호·조회기간·총건수 같은 안내 줄이나
 * 병합 셀 제목 줄이 진짜 표 앞에 몇 줄 붙는 경우가 있다(2026-08-10/2026-08-12, 실제 파일로 확인).
 * **실제 값이 채워진 칸의 개수**가 가장 많은 행을 헤더로 판정한다 — 헤더는 모든 칸에 이름이 채워져
 * 있어 항상 값 개수가 최대라는 성질을 이용한다({@link #findHeaderRowIndex} 참고). 안내 줄이 아예 없는
 * 파일(헤더가 1번째 줄)도 그대로 맞아떨어진다.
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
     * "실제 값이 채워진 칸의 개수"가 가장 많은 행을 헤더로 판정한다.
     * ⚠️ 콤마 개수(폭)가 아니라 값 채워진 칸 개수다 (2026-08-12, 실제 파일로 재수정) — 표 위에 병합 셀
     * 제목("2022년도 매출세금계산서", "거래내역조회" 등)이 붙는 CSV는 그 제목 줄도 표와 똑같은 폭까지
     * 뒤에 빈 콤마가 이어져서, "콤마 개수 최댓값" 기준으로는 제목 줄이 진짜 헤더보다 먼저(또는 동일하게)
     * 잡혀버린다. "콤마가 있다고 칸에 값이 있는 건 아니다"를 놓친 것 — CashFlowExcelParser가 서식만
     * 넓은 안내 줄 문제를 "칸 존재 범위"가 아니라 "값 채워진 칸 개수"로 고친 것과 동일한 이유·동일한
     * 해법이다. 그때는 "CSV는 콤마 특성상 이 문제가 없다"고 판단해 CSV엔 반영하지 않았는데, 그 전제가
     * 틀렸다(콤마 자리는 항상 있어도 그 칸 값은 비어있을 수 있다).
     */
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

    /**
     * 국내 은행 CSV 내보내기는 EUC-KR(엑셀 기본 저장 인코딩)인 경우가 흔하다. BOM이 있으면 UTF-8,
     * 없으면 EUC-KR로 가정한다 — 완벽한 감지는 아니라 실제 은행 CSV 샘플로 나중에 검증이 필요하다.
     */
    /**
     * ⚠️ BOM이 없어도 UTF-8일 수 있다 (2026-08-13, CodeRabbit 지적) — 예전엔 BOM이 없으면 무조건 EUC-KR로
     * 읽었는데, UTF-8 CSV는 BOM 없이 저장되는 게 오히려 흔하다. 그러면 한글 헤더·값이 통째로 깨지고,
     * 에러가 아니라 "깨진 글자로 정상 응답"이 나가서 사용자가 원인을 알 수 없다.
     *
     * <p>그래서 UTF-8로 <b>엄격하게</b> 디코딩을 시도하고, 실패할 때만 EUC-KR로 간다. EUC-KR 한글 바이트는
     * 대부분 유효한 UTF-8 시퀀스가 아니라서 이 판별이 실제로 잘 갈린다(순수 ASCII 파일은 어느 쪽으로 읽어도
     * 결과가 같다).
     */
    private java.nio.charset.Charset resolveCharset(byte[] fileBytes) {
        if (hasUtf8Bom(fileBytes) || isValidUtf8(fileBytes)) {
            return StandardCharsets.UTF_8;
        }
        try {
            return java.nio.charset.Charset.forName("EUC-KR");
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }

    private boolean isValidUtf8(byte[] fileBytes) {
        java.nio.charset.CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT);
        try {
            decoder.decode(java.nio.ByteBuffer.wrap(fileBytes));
            return true;
        } catch (java.nio.charset.CharacterCodingException e) {
            return false;
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
