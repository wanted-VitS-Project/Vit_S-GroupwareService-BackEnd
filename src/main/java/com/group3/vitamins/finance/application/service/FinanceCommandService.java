package com.group3.vitamins.finance.application.service;

import com.group3.vitamins.finance.application.command.CashFlowCsvPreviewCommand;
import com.group3.vitamins.finance.application.command.CashFlowCsvUploadCommand;
import com.group3.vitamins.finance.application.command.CreateCashFlowCommand;
import com.group3.vitamins.finance.application.command.DeleteCashFlowsCommand;
import com.group3.vitamins.finance.application.command.MatchCashFlowCommand;
import com.group3.vitamins.finance.application.command.MatchTaxInvoiceCommand;
import com.group3.vitamins.finance.application.command.TaxInvoiceCsvPreviewCommand;
import com.group3.vitamins.finance.application.command.TaxInvoiceCsvUploadCommand;
import com.group3.vitamins.finance.application.command.UnmatchCashFlowCommand;
import com.group3.vitamins.finance.application.command.UnmatchTaxInvoiceCommand;
import com.group3.vitamins.finance.application.command.UpdateTaxInvoiceMemoCommand;
import com.group3.vitamins.finance.application.command.DeleteTaxInvoicesCommand;
import com.group3.vitamins.finance.application.command.UpdateTaxInvoiceExclusionCommand;
import com.group3.vitamins.finance.application.command.UpdateCashFlowCommand;
import com.group3.vitamins.finance.application.command.UpdateCashFlowExclusionCommand;
import com.group3.vitamins.finance.application.port.PagePermissionPort;
import com.group3.vitamins.finance.application.usecase.FinanceCommandUseCase;
import com.group3.vitamins.finance.domain.exception.FinanceErrorCode;
import com.group3.vitamins.finance.infrastructure.cashflow.CashFlowCommandMapper;
import com.group3.vitamins.finance.infrastructure.cashflow.CashFlowDedupKeyRow;
import com.group3.vitamins.finance.infrastructure.cashflow.CashFlowDeleteCandidateRow;
import com.group3.vitamins.finance.infrastructure.cashflow.CashFlowDetailRow;
import com.group3.vitamins.finance.infrastructure.cashflow.CashFlowMapper;
import com.group3.vitamins.finance.infrastructure.cashflow.CashFlowMatchLookupRow;
import com.group3.vitamins.finance.infrastructure.cashflow.CashFlowMatchResultRow;
import com.group3.vitamins.finance.infrastructure.cashflow.SettlementBlockMatchRow;
import com.group3.vitamins.finance.infrastructure.cashflow.csv.CashFlowAmountMode;
import com.group3.vitamins.finance.infrastructure.cashflow.csv.CashFlowBankCatalog;
import com.group3.vitamins.finance.infrastructure.cashflow.csv.CashFlowCsvColumnRecommender;
import com.group3.vitamins.finance.infrastructure.cashflow.csv.CashFlowCsvMapping;
import com.group3.vitamins.finance.infrastructure.cashflow.csv.CashFlowCsvRecommendation;
import com.group3.vitamins.finance.infrastructure.cashflow.csv.CashFlowCsvRowParser;
import com.group3.vitamins.finance.infrastructure.cashflow.csv.CashFlowCsvTable;
import com.group3.vitamins.finance.infrastructure.cashflow.csv.CashFlowDateTimeMode;
import com.group3.vitamins.finance.infrastructure.cashflow.csv.CashFlowUploadFileReader;
import com.group3.vitamins.finance.infrastructure.cashflow.csv.ParsedCashFlowRow;
import com.group3.vitamins.finance.infrastructure.taxinvoice.TaxInvoiceCommandMapper;
import com.group3.vitamins.finance.infrastructure.taxinvoice.TaxInvoiceMapper;
import com.group3.vitamins.finance.infrastructure.taxinvoice.TaxInvoiceDeleteCandidateRow;
import com.group3.vitamins.finance.infrastructure.taxinvoice.TaxInvoiceMatchLookupRow;
import com.group3.vitamins.finance.infrastructure.taxinvoice.TaxInvoiceMatchResultRow;
import com.group3.vitamins.finance.infrastructure.taxinvoice.TaxInvoiceMemoRow;
import com.group3.vitamins.finance.infrastructure.taxinvoice.csv.ParsedTaxInvoiceRow;
import com.group3.vitamins.finance.infrastructure.taxinvoice.csv.TaxInvoiceCsvColumnRecommender;
import com.group3.vitamins.finance.infrastructure.taxinvoice.csv.TaxInvoiceCsvMapping;
import com.group3.vitamins.finance.infrastructure.taxinvoice.csv.TaxInvoiceCsvRecommendation;
import com.group3.vitamins.finance.infrastructure.taxinvoice.csv.TaxInvoiceCsvRowParser;
import com.group3.vitamins.finance.infrastructure.taxinvoice.csv.TaxInvoiceCsvTable;
import com.group3.vitamins.finance.infrastructure.taxinvoice.csv.TaxInvoiceUploadFileReader;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinanceCommandService implements FinanceCommandUseCase {

    private static final String FINANCE_PAGE_CODE = "FINANCE";
    private static final int SAMPLE_ROW_LIMIT = 5;

    private final PagePermissionPort pagePermissionPort;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;
    private final CashFlowUploadFileReader cashFlowUploadFileReader;
    private final CashFlowCsvColumnRecommender cashFlowCsvColumnRecommender;
    private final CashFlowCsvRowParser cashFlowCsvRowParser;
    private final CashFlowCommandMapper cashFlowCommandMapper;
    private final CashFlowMapper cashFlowMapper;
    private final TaxInvoiceUploadFileReader taxInvoiceUploadFileReader;
    private final TaxInvoiceCsvColumnRecommender taxInvoiceCsvColumnRecommender;
    private final TaxInvoiceCsvRowParser taxInvoiceCsvRowParser;
    private final TaxInvoiceCommandMapper taxInvoiceCommandMapper;
    private final TaxInvoiceMapper taxInvoiceMapper;

    @Override
    @Transactional(readOnly = true)
    public CashFlowCsvPreviewView previewCashFlowCsv(CashFlowCsvPreviewCommand command) {
        log.info("입출금 내역 CSV 컬럼 추천 조회 요청 - userId={}", command.userId());

        assertEditAccess(command.userId(), command.role());

        CashFlowCsvTable table =
                cashFlowUploadFileReader.read(command.fileBytes(), command.fileName(), command.password());
        CashFlowCsvRecommendation recommendation = cashFlowCsvColumnRecommender.recommend(table.headers());

        List<Map<String, String>> sampleRows = table.rows().stream()
                .limit(SAMPLE_ROW_LIMIT)
                .map(row -> toDisplayRow(table.headers(), row))
                .toList();

        return new CashFlowCsvPreviewView(
                table.headers(),
                CashFlowBankCatalog.BANK_OPTIONS,
                sampleRows,
                recommendation.dateTimeMode().name(),
                recommendation.amountMode().name(),
                toMappingView(recommendation.mapping())
        );
    }

    private Map<String, String> toDisplayRow(List<String> headers, Map<String, String> row) {
        // 파싱 단계에서 빈 셀은 null로 정규화해뒀지만(매핑 필수값 체크용), 미리보기 화면에는
        // 원본 CSV처럼 빈 문자열로 보여준다(명세 Success Example의 "출금금액": "" 그대로).
        Map<String, String> displayRow = new LinkedHashMap<>();
        for (String header : headers) {
            String value = row.get(header);
            displayRow.put(header, value == null ? "" : value);
        }
        return displayRow;
    }

    private CashFlowCsvMappingView toMappingView(CashFlowCsvMapping mapping) {
        return new CashFlowCsvMappingView(
                mapping.tradedDateTimeColumn(), mapping.tradedDateColumn(), mapping.tradedTimeColumn(),
                mapping.amountColumn(), mapping.typeColumn(),
                mapping.incomeAmountColumn(), mapping.outcomeAmountColumn(),
                mapping.memoColumn(), mapping.depositorColumn(), mapping.balanceColumn()
        );
    }

    // ⚠️ @Transactional을 일부러 안 붙인다(2026-08-11, CodeRabbit 지적으로 제거) — 파일 파싱(POI/
    // commons-csv, DB 접근 없음)이 이 메서드 앞부분 대부분을 차지하는데, 트랜잭션이 붙어있으면
    // 파싱하는 동안에도 커넥션 풀에서 커넥션을 하나 붙잡고 있게 된다. 동시 업로드가 몇 건만 겹쳐도
    // 커넥션이 낭비된다. insertAll은 여러 행을 한 번에 묶은 단일 INSERT문이라 그 자체로 원자적이고,
    // findExistingDedupKeys(조회)와 굳이 같은 트랜잭션으로 묶을 이유가 없다 — 그 사이의 아주 짧은
    // 동시성 틈은 uk_cash_flow_dedup 유니크 제약이 최종 방어선으로 이미 커버한다(문서에 명시됨).
    @Override
    public CashFlowCsvUploadView uploadCashFlowCsv(CashFlowCsvUploadCommand command) {
        log.info("입출금 내역(CSV 기반) 업로드 요청 - userId={}, bankName={}", command.userId(), command.bankName());

        assertEditAccess(command.userId(), command.role());

        if (!StringUtils.hasText(command.bankName())) {
            throw new ValidationException(FinanceErrorCode.FINANCE_CSV_MAPPING_REQUIRED, "은행명이 필요합니다.");
        }
        CashFlowDateTimeMode dateTimeMode = parseDateTimeMode(command.dateTimeMode());
        CashFlowAmountMode amountMode = parseAmountMode(command.amountMode());
        CashFlowCsvMapping mapping = new CashFlowCsvMapping(
                command.tradedDateTimeColumn(), command.tradedDateColumn(), command.tradedTimeColumn(),
                command.amountColumn(), command.typeColumn(),
                command.incomeAmountColumn(), command.outcomeAmountColumn(),
                command.memoColumn(), command.depositorColumn(), command.balanceColumn()
        );

        CashFlowCsvTable table =
                cashFlowUploadFileReader.read(command.fileBytes(), command.fileName(), command.password());
        validateMapping(table.headers(), dateTimeMode, amountMode, mapping);

        List<ParsedCashFlowRow> parsedRows =
                cashFlowCsvRowParser.parseRows(table, command.bankName(), dateTimeMode, amountMode, mapping);

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        Set<String> seenKeys = new HashSet<>();
        if (!parsedRows.isEmpty()) {
            List<CashFlowDedupKeyRow> existing =
                    cashFlowCommandMapper.findExistingDedupKeys(companyId, command.bankName(), parsedRows);
            existing.forEach(row -> seenKeys.add(dedupKey(row.type(), row.tradedAt(), row.amount(), row.balanceAfter())));
        }

        List<ParsedCashFlowRow> toInsert = new ArrayList<>();
        List<DuplicateRowView> duplicateRows = new ArrayList<>();
        for (ParsedCashFlowRow row : parsedRows) {
            // seenKeys에는 DB에 이미 있는 조합뿐 아니라, 같은 파일 안에서 먼저 처리된 행의 조합도 함께
            // 쌓인다 — 파일 내부 중복도 같은 로직으로 걸러진다.
            if (!seenKeys.add(dedupKey(row.type(), row.tradedAt(), row.amount(), row.balanceAfter()))) {
                duplicateRows.add(new DuplicateRowView(row.tradedAt(), row.amount(), "이미 등록된 거래입니다."));
            } else {
                toInsert.add(row);
            }
        }

        int savedCount = toInsert.isEmpty()
                ? 0
                : insertWithConcurrentDuplicateRetry(companyId, command.bankName(), toInsert, duplicateRows);

        return new CashFlowCsvUploadView(table.rows().size(), savedCount, duplicateRows.size(), duplicateRows);
    }

    /**
     * insertAll은 여러 행을 한 INSERT문으로 묶어 넣는다 — 그중 단 한 행이라도 uk_cash_flow_dedup에
     * 걸리면 배치 전체가 실패한다. 조회(findExistingDedupKeys) 시점엔 없었지만 그 사이 동시에 들어온
     * 다른 요청이 같은 조합을 먼저 커밋한 경우가 여기 해당한다(2026-08-11, CodeRabbit 지적 — 이전엔
     * DuplicateKeyException이 그대로 올라가 500이 노출됐다). 최신 상태로 한 번만 다시 걸러서 재시도한다
     * — 이번엔 지금 이 배치 안에서 조회~삽입 사이 텀이 방금 전보다 훨씬 좁아 재충돌 가능성이 낮고,
     * 그래도 또 걸리면(극히 드묾) 예외를 그대로 던져 무한 재시도를 하지 않는다.
     */
    private int insertWithConcurrentDuplicateRetry(
            Long companyId, String bankName, List<ParsedCashFlowRow> toInsert, List<DuplicateRowView> duplicateRows) {
        try {
            return cashFlowCommandMapper.insertAll(companyId, bankName, toInsert);
        } catch (DuplicateKeyException e) {
            Set<String> latestKeys = new HashSet<>();
            cashFlowCommandMapper.findExistingDedupKeys(companyId, bankName, toInsert)
                    .forEach(row -> latestKeys.add(dedupKey(row.type(), row.tradedAt(), row.amount(), row.balanceAfter())));

            List<ParsedCashFlowRow> retryInsert = new ArrayList<>();
            for (ParsedCashFlowRow row : toInsert) {
                if (latestKeys.contains(dedupKey(row.type(), row.tradedAt(), row.amount(), row.balanceAfter()))) {
                    duplicateRows.add(new DuplicateRowView(row.tradedAt(), row.amount(), "이미 등록된 거래입니다."));
                } else {
                    retryInsert.add(row);
                }
            }
            return retryInsert.isEmpty() ? 0 : cashFlowCommandMapper.insertAll(companyId, bankName, retryInsert);
        }
    }

    /**
     * 은행명·거래일시·금액만으론 "같은 초·같은 금액의 서로 다른 거래"를 구분 못 해서(2026-08-10, 실제
     * 카카오뱅크 CSV로 확인) 잔액도 키에 포함한다. 잔액 컬럼이 없는 CSV는 balanceAfter가 항상 null이라
     * 문자열 "null"로 통일되고, 그 경우 기존과 동일하게 은행명+거래일시+금액만으로 판정된다.
     *
     * ⚠️ type도 반드시 포함해야 한다(2026-08-11, CodeRabbit Critical 지적) — amount는 항상 절댓값으로
     * 저장하므로(parseAmount().abs()), type 없이는 같은 시각·같은 절댓값의 입금/출금 두 건이 완전히
     * 같은 키가 돼서 서로 다른 정상 거래 중 하나가 "이미 등록된 거래"로 조용히 유실된다.
     */
    private String dedupKey(String type, java.time.LocalDateTime tradedAt, java.math.BigDecimal amount, java.math.BigDecimal balanceAfter) {
        String balancePart = balanceAfter == null ? "null" : balanceAfter.stripTrailingZeros().toPlainString();
        return type + "|" + tradedAt + "|" + amount.stripTrailingZeros().toPlainString() + "|" + balancePart;
    }

    private CashFlowDateTimeMode parseDateTimeMode(String raw) {
        try {
            return CashFlowDateTimeMode.valueOf(raw);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ValidationException(FinanceErrorCode.FINANCE_CSV_MAPPING_REQUIRED, "dateTimeMode 값이 올바르지 않습니다.");
        }
    }

    private CashFlowAmountMode parseAmountMode(String raw) {
        try {
            return CashFlowAmountMode.valueOf(raw);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ValidationException(FinanceErrorCode.FINANCE_CSV_MAPPING_REQUIRED, "amountMode 값이 올바르지 않습니다.");
        }
    }

    private void validateMapping(List<String> headers, CashFlowDateTimeMode dateTimeMode,
                                  CashFlowAmountMode amountMode, CashFlowCsvMapping mapping) {
        if (dateTimeMode == CashFlowDateTimeMode.SINGLE) {
            requireColumn(headers, mapping.tradedDateTimeColumn(), "tradedDateTimeColumn");
        } else {
            requireColumn(headers, mapping.tradedDateColumn(), "tradedDateColumn");
            requireColumn(headers, mapping.tradedTimeColumn(), "tradedTimeColumn");
        }

        if (amountMode == CashFlowAmountMode.SINGLE_WITH_TYPE) {
            requireColumn(headers, mapping.amountColumn(), "amountColumn");
            requireColumn(headers, mapping.typeColumn(), "typeColumn");
        } else {
            requireColumn(headers, mapping.incomeAmountColumn(), "incomeAmountColumn");
            requireColumn(headers, mapping.outcomeAmountColumn(), "outcomeAmountColumn");
        }

        // 거래처(depositorColumn)는 모드와 무관하게 항상 필수다 — cash_flow.depositor_name이 NOT NULL이고,
        // 업로드 화면에서도 필수 입력으로 확정됐다(2026-08-10, 원 명세 표는 선택(N)이었으나 정정).
        requireColumn(headers, mapping.depositorColumn(), "depositorColumn");

        // 잔액(balanceColumn)은 선택이다 — 프론트 매핑 화면 설계(날짜/금액/적요/입금자명/은행명만 있고
        // 잔액 드롭다운 자체가 없음, 2026-08-11 확인)상 사용자가 매핑할 방법이 없다. 응답에도 노출 안
        // 되는 순수 DB 내부 중복판정 보강용 값이라, balance_after_present 생성 컬럼으로 NULL이어도
        // 안전하게 처리되는 지금 상태면 필수로 강제할 이유가 없다. 매핑값을 보냈는데 그 컬럼이 실제
        // CSV에 없으면(오타 등) 다른 필수 컬럼과 동일하게 막는다.
        if (StringUtils.hasText(mapping.balanceColumn())) {
            requireColumn(headers, mapping.balanceColumn(), "balanceColumn");
        }
    }

    private void requireColumn(List<String> headers, String column, String fieldName) {
        if (!StringUtils.hasText(column)) {
            throw new ValidationException(FinanceErrorCode.FINANCE_CSV_MAPPING_REQUIRED, fieldName + "이(가) 필요합니다.");
        }
        if (!headers.contains(column)) {
            throw new ValidationException(FinanceErrorCode.FINANCE_CSV_MAPPING_REQUIRED,
                    fieldName + "(" + column + ")이(가) CSV에 없는 컬럼입니다.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TaxInvoiceCsvPreviewView previewTaxInvoiceCsv(TaxInvoiceCsvPreviewCommand command) {
        log.info("세금계산서 CSV 컬럼 추천 조회 요청 - userId={}", command.userId());

        assertEditAccess(command.userId(), command.role());

        TaxInvoiceCsvTable table =
                taxInvoiceUploadFileReader.read(command.fileBytes(), command.fileName(), command.password());
        TaxInvoiceCsvRecommendation recommendation =
                taxInvoiceCsvColumnRecommender.recommend(table.headers(), table.titleText());

        List<Map<String, String>> sampleRows = table.rows().stream()
                .limit(SAMPLE_ROW_LIMIT)
                .map(row -> toDisplayRow(table.headers(), row))
                .toList();

        return new TaxInvoiceCsvPreviewView(
                table.headers(), sampleRows, recommendation.recommendedType(),
                toTaxInvoiceMappingView(recommendation.mapping())
        );
    }

    private TaxInvoiceCsvMappingView toTaxInvoiceMappingView(TaxInvoiceCsvMapping mapping) {
        return new TaxInvoiceCsvMappingView(
                mapping.approvalNoColumn(), mapping.issuedDateColumn(),
                mapping.supplierBizNoColumn(), mapping.buyerBizNoColumn(), mapping.buyerNameColumn(),
                mapping.supplyAmountColumn(), mapping.taxAmountColumn(), mapping.totalAmountColumn(),
                mapping.itemNameColumn(), mapping.ceoNameColumn(), mapping.subBizNoColumn(), mapping.memoColumn()
        );
    }

    // cash_flow의 uploadCashFlowCsv와 동일한 이유로 @Transactional을 안 붙인다 — 파일 파싱은 DB 접근이
    // 없고, insertAll은 단일 INSERT문으로 그 자체가 원자적이다. 조회(findExistingApprovalNos)와 굳이
    // 같은 트랜잭션으로 묶을 이유가 없다 — uk_tax_invoice_approval_no 유니크 제약이 최종 방어선이다.
    @Override
    public TaxInvoiceCsvUploadView uploadTaxInvoiceCsv(TaxInvoiceCsvUploadCommand command) {
        log.info("세금계산서(CSV 기반) 업로드 요청 - userId={}, type={}", command.userId(), command.type());

        assertEditAccess(command.userId(), command.role());

        String type = parseTaxInvoiceType(command.type());
        TaxInvoiceCsvMapping mapping = new TaxInvoiceCsvMapping(
                command.approvalNoColumn(), command.issuedDateColumn(),
                command.supplierBizNoColumn(), command.buyerBizNoColumn(), command.buyerNameColumn(),
                command.supplyAmountColumn(), command.taxAmountColumn(), command.totalAmountColumn(),
                command.itemNameColumn(), command.ceoNameColumn(), command.subBizNoColumn(), command.memoColumn()
        );

        TaxInvoiceCsvTable table =
                taxInvoiceUploadFileReader.read(command.fileBytes(), command.fileName(), command.password());
        validateTaxInvoiceMapping(table.headers(), mapping);

        List<ParsedTaxInvoiceRow> parsedRows = taxInvoiceCsvRowParser.parseRows(table, mapping);

        Set<String> seenApprovalNos = new HashSet<>();
        if (!parsedRows.isEmpty()) {
            List<String> candidates = parsedRows.stream().map(ParsedTaxInvoiceRow::approvalNo).toList();
            seenApprovalNos.addAll(taxInvoiceCommandMapper.findExistingApprovalNos(candidates));
        }

        List<ParsedTaxInvoiceRow> toInsert = new ArrayList<>();
        List<TaxInvoiceDuplicateRowView> duplicateRows = new ArrayList<>();
        for (ParsedTaxInvoiceRow row : parsedRows) {
            // seenApprovalNos에는 DB에 이미 있는 승인번호뿐 아니라, 같은 파일 안에서 먼저 처리된 행의
            // 승인번호도 함께 쌓인다 — 파일 내부 중복도 같은 로직으로 걸러진다.
            if (!seenApprovalNos.add(row.approvalNo())) {
                duplicateRows.add(new TaxInvoiceDuplicateRowView(row.approvalNo(), "이미 등록된 승인번호입니다."));
            } else {
                toInsert.add(row);
            }
        }

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        int savedCount = toInsert.isEmpty()
                ? 0
                : insertTaxInvoicesWithConcurrentDuplicateRetry(companyId, type, toInsert, duplicateRows);

        return new TaxInvoiceCsvUploadView(table.rows().size(), savedCount, duplicateRows.size(), duplicateRows);
    }

    /**
     * 세금계산서를 정산 블록에 연결한다.
     *
     * <p>⭐ 정산 블록 하나에는 <b>입출금 1건 + 세금계산서 1장</b>이 각각 붙고, 두 원장은 서로를 막지
     * 않는다(2026-08-13 확정). 그래서 매칭 가능 판정을 블록 status 로 하지 않는다 — {@code PARTIAL}은
     * "입출금이 붙었다"만 말해주고 세금계산서 유무는 알려주지 않기 때문에, {@code tax_invoice} 를 직접
     * 보는 수밖에 없다. status 는 판정 기준이 아니라 <b>결과값</b>이다:
     * {@code PENDING}이면 {@code WAITING}(정산 대기)으로 올리고, 이미 {@code PARTIAL}/{@code COMPLETED}
     * 면 그대로 둔다. 실적값도 건드리지 않는다(입출금 소관).
     */
    @Override
    @Transactional
    public TaxInvoiceMatchView matchTaxInvoice(MatchTaxInvoiceCommand command) {
        log.info("세금계산서 블록 매칭 요청 - taxId={}, settleId={}, userId={}",
                command.taxId(), command.settleId(), command.userId());

        assertEditAccess(command.userId(), command.role());

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        TaxInvoiceMatchLookupRow taxInvoice = taxInvoiceMapper.findMatchLookup(command.taxId(), companyId);
        if (taxInvoice == null) {
            throw new NotFoundException(FinanceErrorCode.FINANCE_TAX_MATCH_TARGET_NOT_FOUND);
        }
        if (taxInvoice.settleBlockId() != null) {
            throw new ValidationException(FinanceErrorCode.FINANCE_TAX_INVOICE_ALREADY_MATCHED);
        }
        // 제외 처리(is_excluded)된 항목은 "프로젝트와 무관해 미연결 집계에서 뺀 대상"이라 정산 블록에 붙일
        // 수 없다(2026-08-13 추가) — 제외 API가 "매칭된 건 제외 못 함"으로 막는 것과 대칭이다. 붙게 두면
        // 미연결 집계에선 빠져 있는데 정산 현황엔 연결로 보이는 상태가 된다. 해제(unmatch)는 안 막는다 —
        // 이미 붙어 있는 걸 떼는 건 그 모순을 없애는 방향이다.
        if (Boolean.TRUE.equals(taxInvoice.isExcluded())) {
            throw new ValidationException(FinanceErrorCode.FINANCE_TAX_INVOICE_EXCLUDED_CANNOT_MATCH);
        }

        // 회사 스코프 확인 — companyId 없이는 타사 settleId를 그대로 매칭시킬 수 있다(cash_flow와 동일 이유).
        com.group3.vitamins.finance.infrastructure.taxinvoice.SettlementBlockMatchRow settlementBlock =
                taxInvoiceMapper.findSettlementBlockForMatch(command.settleId(), companyId);
        if (settlementBlock == null) {
            throw new NotFoundException(FinanceErrorCode.FINANCE_TAX_MATCH_TARGET_NOT_FOUND);
        }
        // settlementBlock.type()은 아직 항목이 작성된 적 없는 빈 블록이면 null이다 — Objects.equals로
        // null도 안전하게 "불일치"로 처리한다(cash_flow의 matchCashFlow와 동일 이유).
        if (!Objects.equals(settlementBlock.type(), taxInvoice.type())) {
            throw new ValidationException(FinanceErrorCode.FINANCE_TAX_TYPE_MISMATCH);
        }

        // ⚠️ 여기서 블록 행을 잠그고, 잠근 뒤에 다시 확인한다 — 순서가 중요하다.
        // 세금계산서는 이미 PARTIAL/COMPLETED인 블록에 붙을 때 status를 안 바꾸므로, 입출금 매칭처럼
        // "상태 조건부 UPDATE"가 경합 지점이 되어주지 못한다. 잠금 없이 EXISTS만 보면 서로 다른
        // 세금계산서 2건이 같은 블록으로 동시에 들어와 둘 다 통과할 수 있다(블록당 1장 규칙 위반).
        // findLinkedTaxInvoiceId가 잠금 읽기(FOR UPDATE)인 이유는 매퍼 XML 주석 참고.
        taxInvoiceMapper.lockSettlementBlockForUpdate(command.settleId());
        Long linkedTaxId = taxInvoiceMapper.findLinkedTaxInvoiceId(command.settleId());
        if (linkedTaxId != null) {
            throw new ValidationException(FinanceErrorCode.FINANCE_SETTLEMENT_BLOCK_ALREADY_MATCHED);
        }

        LocalDateTime linkedAt = LocalDateTime.now();
        int taxInvoiceUpdated = taxInvoiceCommandMapper.updateTaxInvoiceMatch(
                command.taxId(), command.settleId(), command.userId(), linkedAt);
        if (taxInvoiceUpdated == 0) {
            throw new ValidationException(FinanceErrorCode.FINANCE_TAX_INVOICE_ALREADY_MATCHED);
        }

        // 0행이면 이 블록이 이미 PARTIAL/COMPLETED(입출금이 붙음)라는 뜻 — 정상이므로 확인하지 않는다.
        taxInvoiceCommandMapper.markSettlementBlockWaiting(command.settleId());

        TaxInvoiceMatchResultRow result = taxInvoiceMapper.findMatchResultById(command.taxId(), companyId);
        return new TaxInvoiceMatchView(
                command.taxId(), result.settleId(), result.roundName(), result.projectName(),
                result.linkedBy(), result.linkedByName(), result.linkedAt());
    }

    /**
     * 세금계산서 연결을 해제한다. 입출금이 아직 붙어 있으면 블록 상태·실적값을 그대로 두고,
     * 세금계산서만 붙어 있던 블록({@code WAITING})이면 {@code PENDING}(미연결)으로 되돌린다.
     */
    @Override
    @Transactional
    public void unmatchTaxInvoice(UnmatchTaxInvoiceCommand command) {
        log.info("세금계산서 블록 매칭 해제 요청 - taxId={}, userId={}", command.taxId(), command.userId());

        assertEditAccess(command.userId(), command.role());

        TaxInvoiceMatchLookupRow taxInvoice =
                taxInvoiceMapper.findMatchLookup(command.taxId(), currentCompanyIdProvider.currentCompanyId());
        if (taxInvoice == null) {
            throw new NotFoundException(FinanceErrorCode.FINANCE_TAX_INVOICE_NOT_FOUND);
        }
        if (taxInvoice.settleBlockId() == null) {
            throw new ValidationException(FinanceErrorCode.FINANCE_TAX_INVOICE_NOT_MATCHED);
        }

        taxInvoiceCommandMapper.clearTaxInvoiceMatch(command.taxId());
        taxInvoiceCommandMapper.resetSettlementBlockFromWaiting(taxInvoice.settleBlockId());
    }

    /**
     * 세금계산서 메모 수정. 세금계산서는 수동 등록이 없어 전부 CSV/엑셀로 들어오므로 **메모만** 고칠 수
     * 있다 — 승인번호·금액·사업자번호는 국세청 발급 원본 값이라 손대지 않는다. 입출금 수정 API가
     * {@code sourceType != MANUAL} 인 항목을 메모만 허용하는 것과 같은 규칙이다(그래서 요청 바디에
     * 애초에 memo 하나만 둬서, 다른 필드를 보낼 경로 자체를 없앴다 — 400 분기가 필요 없다).
     *
     * <p>매칭 여부는 보지 않는다 — 입출금도 매칭된 항목의 메모는 항상 수정 가능하다.
     */
    @Override
    @Transactional
    public TaxInvoiceMemoView updateTaxInvoiceMemo(UpdateTaxInvoiceMemoCommand command) {
        log.info("세금계산서 메모 수정 요청 - taxId={}, userId={}", command.taxId(), command.userId());

        assertEditAccess(command.userId(), command.role());

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        if (taxInvoiceMapper.findMemoById(command.taxId(), companyId) == null) {
            throw new NotFoundException(FinanceErrorCode.FINANCE_TAX_INVOICE_NOT_FOUND);
        }

        taxInvoiceCommandMapper.updateTaxInvoiceMemo(command.taxId(), command.memo());

        TaxInvoiceMemoRow result = taxInvoiceMapper.findMemoById(command.taxId(), companyId);
        return new TaxInvoiceMemoView(result.taxId(), result.memo(), result.updatedAt());
    }

    /**
     * 세금계산서 배치 삭제(소프트 삭제). 매칭된 항목은 먼저 매칭을 해제해야 지울 수 있다 — 지워버리면
     * 정산 블록이 참조하던 연결 이력이 붕 뜬다. 처리 못 한 항목은 전체를 실패시키지 않고
     * {@code skippedItems}로 사유와 함께 돌려준다(입출금 배치 삭제와 동일한 구조·규칙).
     */
    @Override
    @Transactional
    public TaxInvoiceDeleteResultView deleteTaxInvoices(DeleteTaxInvoicesCommand command) {
        log.info("세금계산서 배치 삭제 요청 - userId={}, count={}",
                command.userId(), command.taxIds() == null ? 0 : command.taxIds().size());

        assertEditAccess(command.userId(), command.role());

        if (command.taxIds() == null || command.taxIds().isEmpty()) {
            throw new ValidationException(
                    FinanceErrorCode.FINANCE_TAX_INVOICE_REQUIRED_FIELD_MISSING, "삭제할 항목을 선택해주세요.");
        }
        // 같은 ID를 두 번 보내면 IN절은 한 행만 지우는데 deletedCount는 두 번 세게 된다 — 중복 제거 후 처리.
        List<Long> requestedIds = command.taxIds().stream().distinct().toList();

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        // ⚠️ Collectors.toMap은 값이 null이면(미매칭 = settleBlockId null인 정상 케이스) NPE를 던진다
        // (입출금 쪽에서 실제 500으로 겪은 함정) — HashMap.put은 null 값을 허용하므로 직접 채운다.
        Map<Long, Long> settleBlockByTaxId = new HashMap<>();
        for (TaxInvoiceDeleteCandidateRow row : taxInvoiceMapper.findDeleteCandidates(requestedIds, companyId)) {
            settleBlockByTaxId.put(row.taxId(), row.settleBlockId());
        }

        List<Long> deletable = new ArrayList<>();
        List<SkippedTaxInvoiceView> skipped = new ArrayList<>();
        for (Long taxId : requestedIds) {
            if (!settleBlockByTaxId.containsKey(taxId)) {
                skipped.add(new SkippedTaxInvoiceView(
                        taxId, FinanceErrorCode.FINANCE_TAX_INVOICE_NOT_FOUND.getMessage()));
            } else if (settleBlockByTaxId.get(taxId) != null) {
                skipped.add(new SkippedTaxInvoiceView(
                        taxId, FinanceErrorCode.FINANCE_TAX_INVOICE_LINKED_CANNOT_DELETE.getMessage()));
            } else {
                deletable.add(taxId);
            }
        }

        // deletable.size()가 아니라 실제로 지운 행 수를 쓴다 — 조회~삭제 사이에 동시 매칭돼 조건부
        // UPDATE(settle_block_id IS NULL)가 걸러낸 행까지 "삭제 완료"로 보고하면 안 된다.
        int deletedCount = deletable.isEmpty() ? 0 : taxInvoiceCommandMapper.softDeleteBatch(deletable, companyId);

        return new TaxInvoiceDeleteResultView(deletedCount, skipped);
    }

    /**
     * 세금계산서 연결 제외/포함 처리(배치). 입출금 쪽과 동일하게 처리 못 한 항목은 예외로 전체를
     * 되돌리지 않고 {@code skippedItems}로 사유와 함께 돌려준다 — 목록에서 여러 건을 골라 한 번에
     * 처리하는 화면이라 한 건 때문에 전체가 실패하면 쓰기 어렵다.
     */
    @Override
    @Transactional
    public TaxInvoiceExclusionResultView updateTaxInvoiceExclusion(UpdateTaxInvoiceExclusionCommand command) {
        log.info("세금계산서 연결 제외 처리 요청 - userId={}, isExcluded={}, count={}",
                command.userId(), command.isExcluded(),
                command.taxIds() == null ? 0 : command.taxIds().size());

        assertEditAccess(command.userId(), command.role());

        if (command.taxIds() == null || command.taxIds().isEmpty() || command.isExcluded() == null) {
            throw new ValidationException(FinanceErrorCode.FINANCE_TAX_INVOICE_REQUIRED_FIELD_MISSING);
        }
        // 같은 ID를 두 번 보내면 updatedCount가 부풀려진다(입출금 쪽 CodeRabbit 지적과 동일) — 중복 제거.
        List<Long> requestedIds = command.taxIds().stream().distinct().toList();

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        Map<Long, Long> settleBlockByTaxId = new HashMap<>();
        for (TaxInvoiceDeleteCandidateRow row : taxInvoiceMapper.findDeleteCandidates(requestedIds, companyId)) {
            settleBlockByTaxId.put(row.taxId(), row.settleBlockId());
        }

        List<Long> updatable = new ArrayList<>();
        List<SkippedTaxInvoiceView> skipped = new ArrayList<>();
        for (Long taxId : requestedIds) {
            if (!settleBlockByTaxId.containsKey(taxId)) {
                skipped.add(new SkippedTaxInvoiceView(
                        taxId, FinanceErrorCode.FINANCE_TAX_INVOICE_NOT_FOUND.getMessage()));
            } else if (command.isExcluded() && settleBlockByTaxId.get(taxId) != null) {
                // 제외 취소(false)는 매칭 여부와 무관하게 항상 허용 — "이미 매칭됨"은 제외(true)할 때만 막는다.
                skipped.add(new SkippedTaxInvoiceView(
                        taxId, FinanceErrorCode.FINANCE_TAX_INVOICE_LINKED_CANNOT_EXCLUDE.getMessage()));
            } else {
                updatable.add(taxId);
            }
        }

        if (!updatable.isEmpty()) {
            taxInvoiceCommandMapper.updateExcludedBatch(updatable, command.isExcluded(), companyId);
        }

        return new TaxInvoiceExclusionResultView(updatable.size(), skipped);
    }

    /** cash_flow의 insertWithConcurrentDuplicateRetry와 동일한 이유·동일한 구조. */
    private int insertTaxInvoicesWithConcurrentDuplicateRetry(
            Long companyId, String type, List<ParsedTaxInvoiceRow> toInsert, List<TaxInvoiceDuplicateRowView> duplicateRows) {
        try {
            return taxInvoiceCommandMapper.insertAll(companyId, type, toInsert);
        } catch (DuplicateKeyException e) {
            List<String> candidates = toInsert.stream().map(ParsedTaxInvoiceRow::approvalNo).toList();
            Set<String> latestApprovalNos = new HashSet<>(taxInvoiceCommandMapper.findExistingApprovalNos(candidates));

            List<ParsedTaxInvoiceRow> retryInsert = new ArrayList<>();
            for (ParsedTaxInvoiceRow row : toInsert) {
                if (latestApprovalNos.contains(row.approvalNo())) {
                    duplicateRows.add(new TaxInvoiceDuplicateRowView(row.approvalNo(), "이미 등록된 승인번호입니다."));
                } else {
                    retryInsert.add(row);
                }
            }
            return retryInsert.isEmpty() ? 0 : taxInvoiceCommandMapper.insertAll(companyId, type, retryInsert);
        }
    }

    private String parseTaxInvoiceType(String raw) {
        if (!"INCOME".equals(raw) && !"OUTCOME".equals(raw)) {
            throw new ValidationException(FinanceErrorCode.FINANCE_CSV_MAPPING_REQUIRED, "type 값이 올바르지 않습니다.");
        }
        return raw;
    }

    private void validateTaxInvoiceMapping(List<String> headers, TaxInvoiceCsvMapping mapping) {
        requireColumn(headers, mapping.approvalNoColumn(), "approvalNoColumn");
        requireColumn(headers, mapping.issuedDateColumn(), "issuedDateColumn");
        requireColumn(headers, mapping.supplierBizNoColumn(), "supplierBizNoColumn");
        requireColumn(headers, mapping.buyerBizNoColumn(), "buyerBizNoColumn");
        requireColumn(headers, mapping.buyerNameColumn(), "buyerNameColumn");
        requireColumn(headers, mapping.supplyAmountColumn(), "supplyAmountColumn");
        requireColumn(headers, mapping.taxAmountColumn(), "taxAmountColumn");
        requireColumn(headers, mapping.totalAmountColumn(), "totalAmountColumn");

        // itemName/ceoName/subBizNo/memo는 선택 — 보냈으면 실제 CSV에 있는 컬럼인지만 확인한다.
        if (StringUtils.hasText(mapping.itemNameColumn())) {
            requireColumn(headers, mapping.itemNameColumn(), "itemNameColumn");
        }
        if (StringUtils.hasText(mapping.ceoNameColumn())) {
            requireColumn(headers, mapping.ceoNameColumn(), "ceoNameColumn");
        }
        if (StringUtils.hasText(mapping.subBizNoColumn())) {
            requireColumn(headers, mapping.subBizNoColumn(), "subBizNoColumn");
        }
        if (StringUtils.hasText(mapping.memoColumn())) {
            requireColumn(headers, mapping.memoColumn(), "memoColumn");
        }
    }

    @Override
    @Transactional
    public CashFlowMatchView matchCashFlow(MatchCashFlowCommand command) {
        log.info("입출금 내역 블록 매칭 요청 - cashFlowId={}, settleId={}, userId={}",
                command.cashFlowId(), command.settleId(), command.userId());

        assertEditAccess(command.userId(), command.role());

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        CashFlowMatchLookupRow cashFlow = cashFlowMapper.findMatchLookup(command.cashFlowId(), companyId);
        if (cashFlow == null) {
            throw new NotFoundException(FinanceErrorCode.FINANCE_MATCH_TARGET_NOT_FOUND);
        }
        if (cashFlow.settleBlockId() != null) {
            throw new ValidationException(FinanceErrorCode.FINANCE_CASH_FLOW_ALREADY_MATCHED);
        }
        // 제외 처리된 항목은 매칭 불가 — 세금계산서 쪽 matchTaxInvoice와 같은 규칙·같은 이유(2026-08-13 추가).
        if (Boolean.TRUE.equals(cashFlow.isExcluded())) {
            throw new ValidationException(FinanceErrorCode.FINANCE_CASH_FLOW_EXCLUDED_CANNOT_MATCH);
        }

        // 회사 스코프 확인(2026-08-11 추가) — companyId 없이는 타사 settleId를 그대로 매칭시킬 수 있었다.
        SettlementBlockMatchRow settlementBlock = cashFlowMapper.findSettlementBlockForMatch(command.settleId(), companyId);
        if (settlementBlock == null) {
            throw new NotFoundException(FinanceErrorCode.FINANCE_MATCH_TARGET_NOT_FOUND);
        }
        // settlementBlock.type()은 아직 항목이 작성된 적 없는 빈 블록이면 null이다(정산 블록 자체는
        // 존재하지만 PATCH로 내용을 넣기 전까진 type이 정해지지 않음) — Objects.equals로 null도
        // 안전하게 "불일치"로 처리한다(2026-08-10, 실제 테스트로 NPE→500 발견).
        if (!Objects.equals(settlementBlock.type(), cashFlow.type())) {
            throw new ValidationException(FinanceErrorCode.FINANCE_MATCH_TYPE_MISMATCH);
        }
        // 블록당 입출금은 1건뿐이다(2026-08-10 확정) — 부족분은 실무팀이 새 회차를 만들어 매칭한다.
        // ⚠️ WAITING(세금계산서만 붙은 블록)도 허용한다(2026-08-13 규칙 확정) — 세금계산서가 먼저 오고
        // 입금이 나중에 들어오는 게 정상 흐름이라, PENDING만 허용하면 그 블록엔 입금을 못 붙인다.
        // PARTIAL/COMPLETED면 이미 입출금이 붙었다는 뜻이라 거부한다.
        if (!"PENDING".equals(settlementBlock.status()) && !"WAITING".equals(settlementBlock.status())) {
            throw new ValidationException(FinanceErrorCode.FINANCE_SETTLEMENT_BLOCK_ALREADY_MATCHED);
        }

        // 위 두 확인(조회)과 아래 저장 사이에 남이 먼저 매칭할 수 있다 — 정산 블록 쪽을
        // status IN ('PENDING','WAITING') 조건부 UPDATE로 먼저 확정해서 "1건당 1매칭" 규칙을 원자적으로
        // 지키고, 성공했을 때만 cash_flow 쪽을 settle_block_id IS NULL 조건부로 확정한다(2026-08-11,
        // CodeRabbit 지적 — 기존엔 두 UPDATE 모두 조건·영향행 확인이 없어 동시 매칭 시 정산 블록에
        // 두 건이 겹쳐 연결될 수 있었다). 입출금은 매칭 시 status가 반드시 바뀌므로 이 조건부 UPDATE
        // 자체가 경합 지점이 되어준다 — 세금계산서 쪽처럼 별도 행 잠금이 필요 없는 이유다.
        LocalDateTime linkedAt = LocalDateTime.now();
        String status = cashFlow.amount().compareTo(settlementBlock.plannedAmount()) >= 0 ? "COMPLETED" : "PARTIAL";
        int blockUpdated = cashFlowCommandMapper.updateSettlementBlockMatchResult(
                command.settleId(), status, cashFlow.amount(), cashFlow.tradedAt());
        if (blockUpdated == 0) {
            throw new ValidationException(FinanceErrorCode.FINANCE_SETTLEMENT_BLOCK_ALREADY_MATCHED);
        }

        int cashFlowUpdated = cashFlowCommandMapper.updateCashFlowMatch(
                command.cashFlowId(), command.settleId(), command.userId(), linkedAt);
        if (cashFlowUpdated == 0) {
            throw new ValidationException(FinanceErrorCode.FINANCE_CASH_FLOW_ALREADY_MATCHED);
        }

        CashFlowMatchResultRow result = cashFlowMapper.findMatchResultById(command.cashFlowId(), companyId);
        return new CashFlowMatchView(
                command.cashFlowId(), result.settleId(), result.roundName(), result.projectName(),
                result.linkedBy(), result.linkedByName(), result.linkedAt());
    }

    @Override
    @Transactional
    public void unmatchCashFlow(UnmatchCashFlowCommand command) {
        log.info("입출금 내역 블록 매칭 해제 요청 - cashFlowId={}, userId={}", command.cashFlowId(), command.userId());

        assertEditAccess(command.userId(), command.role());

        CashFlowMatchLookupRow cashFlow =
                cashFlowMapper.findMatchLookup(command.cashFlowId(), currentCompanyIdProvider.currentCompanyId());
        if (cashFlow == null) {
            throw new NotFoundException(FinanceErrorCode.FINANCE_CASH_FLOW_NOT_FOUND);
        }
        if (cashFlow.settleBlockId() == null) {
            throw new ValidationException(FinanceErrorCode.FINANCE_CASH_FLOW_NOT_MATCHED);
        }

        cashFlowCommandMapper.clearCashFlowMatch(command.cashFlowId());
        // 세금계산서가 아직 붙어 있으면 PENDING이 아니라 WAITING(정산 대기)으로 되돌린다 — 판정은
        // 쿼리 안에서 한다(2026-08-13 규칙 확정, CashFlowCommandMapper.xml 주석 참고). 실적값은 항상 비운다.
        cashFlowCommandMapper.resetSettlementBlockMatch(cashFlow.settleBlockId());
    }

    @Override
    @Transactional
    public CashFlowDetailView createCashFlow(CreateCashFlowCommand command) {
        log.info("입출금 내역 직접 등록 요청 - userId={}, bankName={}", command.userId(), command.bankName());

        assertEditAccess(command.userId(), command.role());

        if (!StringUtils.hasText(command.bankName()) || command.tradedAt() == null
                || command.amount() == null || !StringUtils.hasText(command.depositorName())) {
            throw new ValidationException(FinanceErrorCode.FINANCE_CASH_FLOW_REQUIRED_FIELD_MISSING);
        }
        // 방향은 type이 전담한다 — amount에 0 이하 값이 섞이면 중복 판정·매칭 시 실적 비교(§matchCashFlow)가
        // 어긋난다(2026-08-11, CodeRabbit 지적).
        if (command.amount().signum() <= 0) {
            throw new ValidationException(FinanceErrorCode.FINANCE_CASH_FLOW_AMOUNT_INVALID);
        }
        String type = validateType(command.type());

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        if (cashFlowMapper.existsDuplicate(companyId, command.bankName(), type, command.tradedAt(), command.amount())) {
            throw new ConflictException(FinanceErrorCode.FINANCE_CASH_FLOW_DUPLICATE);
        }

        String bankTxnId = generateBankTxnId(command.bankName(), command.tradedAt());
        cashFlowCommandMapper.insertManual(companyId, command.bankName(), type, command.tradedAt(),
                command.amount(), command.depositorName(), command.memo(), bankTxnId);
        Long cashFlowId = cashFlowCommandMapper.lastInsertedId();

        return toDetailView(cashFlowMapper.findDetailById(cashFlowId, companyId));
    }

    @Override
    @Transactional
    public CashFlowDetailView updateCashFlow(UpdateCashFlowCommand command) {
        log.info("입출금 내역 수정 요청 - cashFlowId={}, userId={}", command.cashFlowId(), command.userId());

        assertEditAccess(command.userId(), command.role());

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        CashFlowDetailRow current = cashFlowMapper.findDetailById(command.cashFlowId(), companyId);
        if (current == null) {
            throw new NotFoundException(FinanceErrorCode.FINANCE_CASH_FLOW_NOT_FOUND);
        }

        // CSV/API 출처이거나 이미 정산 블록에 매칭된 직접 등록 항목은 메모만 수정 가능(요청사항).
        boolean memoOnly = !"MANUAL".equals(current.sourceType()) || current.settleBlockId() != null;
        boolean hasOtherFields = command.bankName() != null || command.tradedAt() != null
                || command.type() != null || command.amount() != null || command.depositorName() != null;

        if (memoOnly) {
            if (hasOtherFields) {
                throw new ValidationException(FinanceErrorCode.FINANCE_CASH_FLOW_FIELD_EDIT_NOT_ALLOWED);
            }
            cashFlowCommandMapper.updateCashFlowMemo(command.cashFlowId(), command.memo());
        } else {
            String type = command.type() != null ? validateType(command.type()) : current.type();
            String bankName = command.bankName() != null ? command.bankName() : current.bankName();
            LocalDateTime tradedAt = command.tradedAt() != null ? command.tradedAt() : current.tradedAt();
            BigDecimal amount = command.amount() != null ? command.amount() : current.amount();
            String depositorName = command.depositorName() != null ? command.depositorName() : current.depositorName();
            String memo = command.memo() != null ? command.memo() : current.memo();

            if (amount.signum() <= 0) {
                throw new ValidationException(FinanceErrorCode.FINANCE_CASH_FLOW_AMOUNT_INVALID);
            }

            // 식별 필드(은행명·구분·거래일시·금액)가 바뀌는 경우에만 중복 재검사한다 — 메모만 바뀌는
            // 흔한 경우까지 매번 검사하지 않기 위함. ⚠️ type도 포함해야 한다(2026-08-11, CodeRabbit
            // 지적) — type만 바꿔서 다른 기존 거래와 (은행+거래일시+금액)이 겹쳐도 이 조건에 없으면
            // 재검사를 안 타서 중복이 그대로 저장됐다.
            boolean identityChanged = command.bankName() != null || command.type() != null
                    || command.tradedAt() != null || command.amount() != null;
            if (identityChanged
                    && cashFlowMapper.existsDuplicateExcluding(command.cashFlowId(), companyId, bankName, type, tradedAt, amount)) {
                throw new ConflictException(FinanceErrorCode.FINANCE_CASH_FLOW_DUPLICATE);
            }

            cashFlowCommandMapper.updateCashFlowManual(
                    command.cashFlowId(), bankName, tradedAt, type, amount, depositorName, memo);
        }

        return toDetailView(cashFlowMapper.findDetailById(command.cashFlowId(), companyId));
    }

    @Override
    @Transactional
    public CashFlowDeleteResultView deleteCashFlows(DeleteCashFlowsCommand command) {
        log.info("입출금 내역 배치 삭제 요청 - userId={}, count={}",
                command.userId(), command.cashFlowIds() == null ? 0 : command.cashFlowIds().size());

        assertEditAccess(command.userId(), command.role());

        if (command.cashFlowIds() == null || command.cashFlowIds().isEmpty()) {
            throw new ValidationException(FinanceErrorCode.FINANCE_CASH_FLOW_REQUIRED_FIELD_MISSING, "삭제할 항목을 선택해주세요.");
        }
        // 같은 ID를 두 번 보내면 SQL IN절은 한 행만 지우는데 deletedCount는 두 번 세게 된다
        // (2026-08-11, CodeRabbit 지적) — 중복 제거 후 처리한다.
        List<Long> requestedIds = command.cashFlowIds().stream().distinct().toList();

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        // Collectors.toMap은 내부적으로 Map.merge를 써서 값이 null이면(미매칭 = settleBlockId null인
        // 정상 케이스) NullPointerException을 던진다(2026-08-10, 실제 테스트로 500 발견) — HashMap.put은
        // null 값을 허용하므로 직접 채운다.
        Map<Long, Long> settleBlockByCashFlowId = new HashMap<>();
        for (CashFlowDeleteCandidateRow row : cashFlowMapper.findDeleteCandidates(requestedIds, companyId)) {
            settleBlockByCashFlowId.put(row.cashFlowId(), row.settleBlockId());
        }

        List<Long> deletable = new ArrayList<>();
        List<SkippedCashFlowView> skipped = new ArrayList<>();
        for (Long cashFlowId : requestedIds) {
            if (!settleBlockByCashFlowId.containsKey(cashFlowId)) {
                skipped.add(new SkippedCashFlowView(cashFlowId, FinanceErrorCode.FINANCE_CASH_FLOW_NOT_FOUND.getMessage()));
            } else if (settleBlockByCashFlowId.get(cashFlowId) != null) {
                skipped.add(new SkippedCashFlowView(cashFlowId, FinanceErrorCode.FINANCE_CASH_FLOW_LINKED_CANNOT_DELETE.getMessage()));
            } else {
                deletable.add(cashFlowId);
            }
        }

        // softDeleteBatch가 실제로 지운 행 수를 그대로 쓴다 — deletable.size()를 그대로 응답하면
        // 조회(findDeleteCandidates)~삭제 사이에 동시 매칭돼 조건부 UPDATE가 걸러낸 행까지
        // "삭제 완료"로 잘못 보고하게 된다(2026-08-11, CodeRabbit 지적).
        int deletedCount = deletable.isEmpty() ? 0 : cashFlowCommandMapper.softDeleteBatch(deletable, companyId);

        return new CashFlowDeleteResultView(deletedCount, skipped);
    }

    @Override
    @Transactional
    public CashFlowExclusionResultView updateCashFlowExclusion(UpdateCashFlowExclusionCommand command) {
        log.info("입출금 내역 연결 제외 처리 요청 - userId={}, isExcluded={}, count={}",
                command.userId(), command.isExcluded(),
                command.cashFlowIds() == null ? 0 : command.cashFlowIds().size());

        assertEditAccess(command.userId(), command.role());

        if (command.cashFlowIds() == null || command.cashFlowIds().isEmpty() || command.isExcluded() == null) {
            throw new ValidationException(FinanceErrorCode.FINANCE_CASH_FLOW_REQUIRED_FIELD_MISSING);
        }
        // 같은 ID 중복 입력 시 updatedCount가 부풀려지는 문제(2026-08-11, CodeRabbit 지적) — 중복 제거.
        List<Long> requestedIds = command.cashFlowIds().stream().distinct().toList();

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        Map<Long, Long> settleBlockByCashFlowId = new HashMap<>();
        for (CashFlowDeleteCandidateRow row : cashFlowMapper.findDeleteCandidates(requestedIds, companyId)) {
            settleBlockByCashFlowId.put(row.cashFlowId(), row.settleBlockId());
        }

        List<Long> updatable = new ArrayList<>();
        List<SkippedCashFlowView> skipped = new ArrayList<>();
        for (Long cashFlowId : requestedIds) {
            if (!settleBlockByCashFlowId.containsKey(cashFlowId)) {
                skipped.add(new SkippedCashFlowView(cashFlowId, FinanceErrorCode.FINANCE_CASH_FLOW_NOT_FOUND.getMessage()));
            } else if (command.isExcluded() && settleBlockByCashFlowId.get(cashFlowId) != null) {
                // 제외 취소(false)는 매칭 여부와 무관하게 항상 허용 — "이미 매칭됨"은 제외(true)할 때만 막는다.
                skipped.add(new SkippedCashFlowView(
                        cashFlowId, FinanceErrorCode.FINANCE_CASH_FLOW_LINKED_CANNOT_EXCLUDE.getMessage()));
            } else {
                updatable.add(cashFlowId);
            }
        }

        if (!updatable.isEmpty()) {
            cashFlowCommandMapper.updateExcludedBatch(updatable, command.isExcluded(), companyId);
        }

        return new CashFlowExclusionResultView(updatable.size(), skipped);
    }

    private String validateType(String raw) {
        if (!"INCOME".equals(raw) && !"OUTCOME".equals(raw)) {
            throw new ValidationException(FinanceErrorCode.FINANCE_CASH_FLOW_REQUIRED_FIELD_MISSING, "type 값이 올바르지 않습니다.");
        }
        return raw;
    }

    // 같은 은행·같은 초에 금액/type만 다른 수동 거래는 중복 검사는 통과하는데 base가 같아진다
    // (bank_txn_id엔 유니크 제약이 없어 예외는 안 나지만, 서로 다른 거래가 같은 참조번호로 표시된다 —
    // 2026-08-11, CodeRabbit 지적). CSV 경로(CashFlowCsvRowParser)는 배치 내 시퀀스 카운터로 구분하는데
    // 수동 등록은 한 번에 한 건씩 들어와 그 방식을 못 쓰므로, 짧은 랜덤 접미사로 유일성을 보장한다.
    private String generateBankTxnId(String bankName, LocalDateTime tradedAt) {
        String prefix = bankName.substring(0, Math.min(4, bankName.length()));
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return prefix + "-" + tradedAt.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-" + suffix;
    }

    private CashFlowDetailView toDetailView(CashFlowDetailRow row) {
        return new CashFlowDetailView(
                row.cashFlowId(), row.bankTxnId(), row.bankName(), row.tradedAt(), row.type(), row.amount(),
                row.depositorName(), row.memo(), row.sourceType(), row.createdAt(), row.updatedAt()
        );
    }

    private void assertEditAccess(String userId, String role) {
        if (!pagePermissionPort.hasEditAccess(FINANCE_PAGE_CODE, userId, role)) {
            log.warn("재무 관리 페이지 편집 권한 없음 - userId={}", userId);
            throw new ForbiddenException(FinanceErrorCode.FINANCE_EDIT_ACCESS_DENIED);
        }
    }
}
