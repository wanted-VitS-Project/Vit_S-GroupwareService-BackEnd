package com.group3.vitamins.finance.infrastructure.cashflow.csv;

import com.group3.vitamins.finance.domain.exception.FinanceErrorCode;
import com.group3.vitamins.global.domain.common.error.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SEPARATE(거래일자·거래시간 분리) 모드에서 시간 컬럼 값을 어떻게 읽는지에 대한 회귀 테스트.
 *
 * <p>엑셀이 시각 전용 셀을 "0일차 + 시각"으로 저장해서 읽으면 기준일 1899-12-31이 따라붙는 문제가
 * 있었고(2026-08-13 프론트 제보), 그걸 흡수하면서 접두부를 검증하지 않아 깨진 값까지 통과하던 문제를
 * 다시 보강했다(CodeRabbit 지적). 두 방향 모두 여기서 고정한다.
 */
@DisplayName("CashFlowCsvRowParser SEPARATE 모드 시간 컬럼 파싱")
class CashFlowCsvRowParserTimeColumnTest {

    private static final String DATE_COLUMN = "거래일자";
    private static final String TIME_COLUMN = "거래시간";
    private static final String DEPOSITOR_COLUMN = "내용";
    private static final String INCOME_COLUMN = "입금(원)";
    private static final String OUTCOME_COLUMN = "출금(원)";

    private final CashFlowCsvRowParser parser = new CashFlowCsvRowParser();

    @Test
    @DisplayName("시각만 있는 값은 그대로 날짜 컬럼과 합쳐진다")
    void parsesPlainTime() {
        List<ParsedCashFlowRow> rows = parseWithTime("11:20:15");

        assertThat(rows).singleElement()
                .extracting(ParsedCashFlowRow::tradedAt)
                .isEqualTo(LocalDateTime.of(2026, 8, 5, 11, 20, 15));
    }

    @Test
    @DisplayName("엑셀 시각 전용 셀의 1899-12-31 접두부는 버리고 시각만 취한다")
    void ignoresExcelEpochDatePrefix() {
        List<ParsedCashFlowRow> rows = parseWithTime("1899-12-31 11:20:15");

        assertThat(rows).singleElement()
                .extracting(ParsedCashFlowRow::tradedAt)
                .isEqualTo(LocalDateTime.of(2026, 8, 5, 11, 20, 15));
    }

    @Test
    @DisplayName("날짜 컬럼과 다른 날짜가 접두부로 와도 날짜 컬럼 값이 이긴다")
    void dateColumnWinsOverPrefix() {
        List<ParsedCashFlowRow> rows = parseWithTime("2020-01-01 09:00:00");

        assertThat(rows).singleElement()
                .extracting(ParsedCashFlowRow::tradedAt)
                .isEqualTo(LocalDateTime.of(2026, 8, 5, 9, 0, 0));
    }

    @Test
    @DisplayName("날짜로 해석되지 않는 접두부가 붙어 있으면 거부한다")
    void rejectsUnparsableDatePrefix() {
        assertThatThrownBy(() -> parseWithTime("잘못된 날짜 11:20:15"))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", FinanceErrorCode.FINANCE_CSV_MAPPING_REQUIRED);
    }

    @Test
    @DisplayName("시각으로 해석되지 않는 값은 거부한다")
    void rejectsUnparsableTime() {
        assertThatThrownBy(() -> parseWithTime("오전 열한시"))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", FinanceErrorCode.FINANCE_CSV_MAPPING_REQUIRED);
    }

    private List<ParsedCashFlowRow> parseWithTime(String timeValue) {
        CashFlowCsvTable table = new CashFlowCsvTable(
                List.of(DATE_COLUMN, TIME_COLUMN, INCOME_COLUMN, OUTCOME_COLUMN, DEPOSITOR_COLUMN),
                // 출금 컬럼은 값이 없는 입금 행 — 실제 파서도 빈 칸을 null로 만들어 넘긴다(키 자체를 뺀 것과 동일).
                List.of(Map.of(
                        DATE_COLUMN, "2026-08-05",
                        TIME_COLUMN, timeValue,
                        INCOME_COLUMN, "30000000",
                        DEPOSITOR_COLUMN, "2차 기성금")));

        CashFlowCsvMapping mapping = new CashFlowCsvMapping(
                null, DATE_COLUMN, TIME_COLUMN, null, null,
                INCOME_COLUMN, OUTCOME_COLUMN, null, DEPOSITOR_COLUMN, null);

        return parser.parseRows(table, "신한은행",
                CashFlowDateTimeMode.SEPARATE, CashFlowAmountMode.SEPARATE, mapping);
    }
}
