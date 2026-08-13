package com.group3.vitamins.finance.infrastructure.cashflow.csv;

import com.group3.vitamins.finance.domain.exception.FinanceErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 확정된 컬럼 매핑으로 CSV 행을 실제 저장 형태(ParsedCashFlowRow)로 변환한다.
 *
 * ⚠️ 날짜·시간 포맷, "구분" 컬럼의 입금/출금 판별 키워드는 명세에 규칙이 없어 직접 설계했다 —
 * 실제 은행 CSV 샘플이 들어오면 포맷 목록을 검증·보강해야 한다.
 */
@Component
public class CashFlowCsvRowParser {

    private static final List<DateTimeFormatter> DATETIME_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyyMMdd HHmmss"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    );
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd")
    );
    private static final List<DateTimeFormatter> TIME_FORMATS = List.of(
            DateTimeFormatter.ofPattern("HH:mm:ss"),
            DateTimeFormatter.ofPattern("HH:mm"),
            DateTimeFormatter.ofPattern("HHmmss")
    );

    private static final List<String> INCOME_TYPE_KEYWORDS = List.of("입금", "INCOME");
    private static final List<String> OUTCOME_TYPE_KEYWORDS = List.of("출금", "OUTCOME");

    public List<ParsedCashFlowRow> parseRows(
            CashFlowCsvTable table, String bankName,
            CashFlowDateTimeMode dateTimeMode, CashFlowAmountMode amountMode, CashFlowCsvMapping mapping) {

        List<ParsedCashFlowRow> result = new ArrayList<>();
        Map<String, Integer> bankTxnIdSeq = new HashMap<>();

        for (Map<String, String> row : table.rows()) {
            LocalDateTime tradedAt = parseTradedAt(row, dateTimeMode, mapping);

            AmountAndType amountAndType = parseAmountAndType(row, amountMode, mapping);
            if (amountAndType == null) {
                // 금액 컬럼이 둘 다 비어있는 행(공백 줄 등)은 건너뛴다 — 저장 대상이 아니다.
                continue;
            }

            // 거래처(depositorName)는 cash_flow.depositor_name이 NOT NULL이기도 하고, 업로드 화면에서
            // 필수로 받기로 확정됐다(2026-08-10) — depositorColumn 매핑 자체가 필수다(validateMapping에서 검증).
            String depositorName = requireValue(row, mapping.depositorColumn(), "depositorColumn");
            String bankMemo = valueOf(row, mapping.memoColumn());
            String bankTxnId = generateBankTxnId(bankName, tradedAt, bankTxnIdSeq);
            BigDecimal balanceAfter = parseBalanceIfPresent(row, mapping.balanceColumn());

            result.add(new ParsedCashFlowRow(
                    tradedAt, amountAndType.type(), amountAndType.amount(), balanceAfter,
                    depositorName, bankMemo, bankTxnId));
        }
        return result;
    }

    private LocalDateTime parseTradedAt(Map<String, String> row, CashFlowDateTimeMode mode, CashFlowCsvMapping mapping) {
        if (mode == CashFlowDateTimeMode.SEPARATE) {
            String dateRaw = requireValue(row, mapping.tradedDateColumn(), "tradedDateColumn");
            String timeRaw = valueOf(row, mapping.tradedTimeColumn());
            LocalDate date = parseDate(dateRaw);
            LocalTime time = timeRaw != null ? parseTime(timeRaw) : LocalTime.MIDNIGHT;
            return LocalDateTime.of(date, time);
        }

        String raw = requireValue(row, mapping.tradedDateTimeColumn(), "tradedDateTimeColumn");
        for (DateTimeFormatter format : DATETIME_FORMATS) {
            try {
                return LocalDateTime.parse(raw, format);
            } catch (DateTimeParseException ignored) {
                // 다음 포맷 시도
            }
        }
        // 시간 없이 날짜만 있는 경우(추천 로직이 "날짜" 컬럼을 통합 컬럼으로 추천하는 케이스) 자정으로 간주한다.
        return parseDate(raw).atStartOfDay();
    }

    private LocalDate parseDate(String raw) {
        LocalDate date = parseDateOrNull(raw);
        if (date == null) {
            throw new ValidationException(FinanceErrorCode.FINANCE_CSV_MAPPING_REQUIRED,
                    "날짜 형식을 해석할 수 없습니다: " + raw);
        }
        return date;
    }

    private LocalDate parseDateOrNull(String raw) {
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                return LocalDate.parse(raw, format);
            } catch (DateTimeParseException ignored) {
                // 다음 포맷 시도
            }
        }
        return null;
    }

    private LocalTime parseTimeOrNull(String raw) {
        for (DateTimeFormatter format : TIME_FORMATS) {
            try {
                return LocalTime.parse(raw, format);
            } catch (DateTimeParseException ignored) {
                // 다음 포맷 시도
            }
        }
        return null;
    }

    /**
     * SEPARATE 모드의 시간 컬럼은 **날짜 성분을 무시하고 시각만** 취한다.
     *
     * <p>값에 날짜가 붙어 오는 경우가 실제로 있다 (2026-08-13, 프론트 제보) — 엑셀은 시각만 넣은 셀을
     * "0일차 + 시각"으로 저장해서 읽으면 기준일(1899-12-31)이 따라붙는다. 파서 쪽에서 이미 시각만
     * 남기도록 고쳤지만, 날짜+시간이 통째로 들어있는 CSV가 이 컬럼으로 매핑될 수도 있어 여기서도 한 번
     * 더 흡수한다 — 날짜 컬럼이 따로 있는 모드라 시간 컬럼의 날짜 성분은 어차피 버리는 값이다.
     *
     * <p>⚠️ **접두부는 "실제로 날짜로 해석되는 경우"에만 떼어낸다** (2026-08-13, CodeRabbit 지적으로 보강).
     * 처음엔 마지막 공백 뒤를 무조건 시각으로 읽었는데, 그러면 {@code "잘못된 날짜 11:20:15"} 같은 깨진
     * 값도 조용히 통과해서 정상 거래 시각으로 저장된다. 접두부가 {@link #DATE_FORMATS}로 안 읽히면
     * 흡수하지 않고 {@code FINANCE_CSV_MAPPING_REQUIRED}로 거부한다.
     */
    private LocalTime parseTime(String raw) {
        LocalTime time = parseTimeOrNull(raw);
        if (time != null) {
            return time;
        }

        // "1899-12-31 11:20:15"처럼 날짜가 앞에 붙은 형태 — 접두부가 날짜로 확인될 때만 떼어낸다.
        int lastSpace = raw.lastIndexOf(' ');
        if (lastSpace > 0 && lastSpace < raw.length() - 1 && parseDateOrNull(raw.substring(0, lastSpace)) != null) {
            time = parseTimeOrNull(raw.substring(lastSpace + 1));
            if (time != null) {
                return time;
            }
        }

        // 날짜·시각 구분자가 공백이 아닌 형태(ISO의 'T' 등) — 포맷 전체가 맞아떨어질 때만 통과한다.
        for (DateTimeFormatter format : DATETIME_FORMATS) {
            try {
                return LocalDateTime.parse(raw, format).toLocalTime();
            } catch (DateTimeParseException ignored) {
                // 다음 포맷 시도
            }
        }

        throw new ValidationException(FinanceErrorCode.FINANCE_CSV_MAPPING_REQUIRED,
                "시간 형식을 해석할 수 없습니다: " + raw);
    }

    private record AmountAndType(String type, BigDecimal amount) {
    }

    private AmountAndType parseAmountAndType(Map<String, String> row, CashFlowAmountMode mode, CashFlowCsvMapping mapping) {
        if (mode == CashFlowAmountMode.SEPARATE) {
            // 은행이 값 없는 쪽을 빈 칸이 아니라 문자 그대로 "0"으로 채워 내려주는 경우가 흔하다
            // (2026-08-10, 실제 파일로 확인 — 엑셀 서식 때문에 빈칸처럼 보였을 뿐 원본 값은 "0").
            // null뿐 아니라 0원도 "이 쪽엔 거래 없음"으로 취급해야 반대쪽을 옳게 판정한다.
            String incomeRaw = valueOf(row, mapping.incomeAmountColumn());
            String outcomeRaw = valueOf(row, mapping.outcomeAmountColumn());
            BigDecimal income = incomeRaw == null ? null : parseAmount(incomeRaw);
            BigDecimal outcome = outcomeRaw == null ? null : parseAmount(outcomeRaw);
            boolean hasIncome = income != null && income.compareTo(BigDecimal.ZERO) != 0;
            boolean hasOutcome = outcome != null && outcome.compareTo(BigDecimal.ZERO) != 0;
            if (hasIncome) {
                return new AmountAndType("INCOME", income);
            }
            if (hasOutcome) {
                return new AmountAndType("OUTCOME", outcome);
            }
            return null;
        }

        String amountRaw = requireValue(row, mapping.amountColumn(), "amountColumn");
        String typeRaw = requireValue(row, mapping.typeColumn(), "typeColumn");
        String type = classifyType(typeRaw);
        return new AmountAndType(type, parseAmount(amountRaw));
    }

    /**
     * ⚠️ 1글자 키워드("입"/"출")는 부분 문자열(contains)이 아니라 완전 일치로만 인정한다
     * (2026-08-11, CodeRabbit 지적으로 수정) — "카드매입"처럼 무관한 단어 안에 "입" 한 글자가
     * 우연히 들어있으면 출금인데 입금으로 오판정됐다. "입금"/"출금"/"INCOME"/"OUTCOME"은 부분
     * 일치를 유지한다(온전한 단어라 오판정 위험이 낮음, 예: "이체입금"도 정상 인식). 어느 쪽에도
     * 안 걸리면 조용히 잘못 저장하지 않고 명확히 에러를 던진다.
     */
    private String classifyType(String raw) {
        String trimmed = raw.trim();
        String upper = trimmed.toUpperCase(java.util.Locale.ROOT);
        if (INCOME_TYPE_KEYWORDS.stream().anyMatch(k -> trimmed.contains(k) || upper.contains(k))) {
            return "INCOME";
        }
        if (OUTCOME_TYPE_KEYWORDS.stream().anyMatch(k -> trimmed.contains(k) || upper.contains(k))) {
            return "OUTCOME";
        }
        if (trimmed.equals("입")) {
            return "INCOME";
        }
        if (trimmed.equals("출")) {
            return "OUTCOME";
        }
        throw new ValidationException(FinanceErrorCode.FINANCE_CSV_MAPPING_REQUIRED,
                "구분 값을 입금/출금으로 해석할 수 없습니다: " + raw);
    }

    /**
     * 항상 절댓값(양수)으로 돌려준다 — 방향(입금/출금)은 type 필드가 전담한다. 은행에 따라 "거래금액"에
     * 부호(예: "-10,000")를 같이 싣는 경우가 있는데(2026-08-10, 실제 파일로 확인), 원본 CSV/엑셀 값 자체는
     * 그대로 읽되 DB에 저장하는 시점에만 부호를 떼어낸다 — 그대로 두면 type과 부호가 이중으로 방향을
     * 표현하게 돼 나중에 합계 계산이 꼬인다.
     */
    private BigDecimal parseAmount(String raw) {
        try {
            return new BigDecimal(raw.replace(",", "").trim()).abs();
        } catch (NumberFormatException e) {
            throw new ValidationException(FinanceErrorCode.FINANCE_CSV_MAPPING_REQUIRED,
                    "금액 값을 숫자로 해석할 수 없습니다: " + raw);
        }
    }

    /** 잔액은 선택 매핑이다 — 없거나 셀이 비어도 통과, 부호는 원본 그대로 둔다(방향 개념이 아니라서 절댓값 처리 불필요). */
    private BigDecimal parseBalanceIfPresent(Map<String, String> row, String balanceColumn) {
        String raw = valueOf(row, balanceColumn);
        if (raw == null) {
            return null;
        }
        try {
            return new BigDecimal(raw.replace(",", "").trim());
        } catch (NumberFormatException e) {
            throw new ValidationException(FinanceErrorCode.FINANCE_CSV_MAPPING_REQUIRED,
                    "잔액 값을 숫자로 해석할 수 없습니다: " + raw);
        }
    }

    private String generateBankTxnId(String bankName, LocalDateTime tradedAt, Map<String, Integer> seq) {
        String prefix = bankName.substring(0, Math.min(4, bankName.length()));
        String base = prefix + "-" + tradedAt.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int count = seq.merge(base, 1, Integer::sum);
        return count == 1 ? base : base + "-" + count;
    }

    private String valueOf(Map<String, String> row, String column) {
        return column == null ? null : row.get(column);
    }

    private String requireValue(Map<String, String> row, String column, String fieldName) {
        String value = valueOf(row, column);
        if (value == null) {
            throw new ValidationException(FinanceErrorCode.FINANCE_CSV_MAPPING_REQUIRED,
                    fieldName + " 매핑 컬럼(" + column + ")에 값이 없는 행이 있습니다.");
        }
        return value;
    }
}
