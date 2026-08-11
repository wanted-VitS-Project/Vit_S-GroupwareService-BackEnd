package com.group3.vitamins.finance.infrastructure.cashflow;

import com.group3.vitamins.finance.infrastructure.cashflow.csv.ParsedCashFlowRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** CSV 업로드 저장·매칭/매칭 해제 전용. 조회는 CashFlowMapper 소관, 이쪽은 쓰기만 담당한다. */
@Mapper
public interface CashFlowCommandMapper {

    /** candidates 중 이미 DB에 존재하는 (tradedAt, amount) 조합만 골라 돌려준다(같은 은행·회사 범위). */
    List<CashFlowDedupKeyRow> findExistingDedupKeys(
            @Param("companyId") Long companyId,
            @Param("bankName") String bankName,
            @Param("candidates") List<ParsedCashFlowRow> candidates);

    /** 중복 제거가 끝난 행만 일괄 INSERT한다. 반환값은 실제 삽입된 행 수. */
    int insertAll(
            @Param("companyId") Long companyId,
            @Param("bankName") String bankName,
            @Param("rows") List<ParsedCashFlowRow> rows);

    /** cash_flow 쪽 연결 정보 저장. */
    int updateCashFlowMatch(
            @Param("cashFlowId") Long cashFlowId,
            @Param("settleId") Long settleId,
            @Param("linkedBy") String linkedBy,
            @Param("linkedAt") LocalDateTime linkedAt);

    /** 정산 블록 쪽 결과 반영 — status(PARTIAL/COMPLETED)·actual_amount·actual_date. */
    int updateSettlementBlockMatchResult(
            @Param("settleId") Long settleId,
            @Param("status") String status,
            @Param("actualAmount") BigDecimal actualAmount,
            @Param("actualDate") LocalDateTime actualDate);

    /** cash_flow 쪽 연결 정보 해제. */
    int clearCashFlowMatch(@Param("cashFlowId") Long cashFlowId);

    /** 정산 블록을 PENDING으로 되돌리고 실적값을 비운다. */
    int resetSettlementBlockMatch(@Param("settleId") Long settleId);

    /** 직접 등록(MANUAL). 생성된 ID는 lastInsertedId()로 별도 조회한다. */
    int insertManual(
            @Param("companyId") Long companyId,
            @Param("bankName") String bankName,
            @Param("type") String type,
            @Param("tradedAt") LocalDateTime tradedAt,
            @Param("amount") BigDecimal amount,
            @Param("depositorName") String depositorName,
            @Param("memo") String memo,
            @Param("bankTxnId") String bankTxnId);

    /** insertManual 직후 같은 커넥션에서 호출 — MySQL LAST_INSERT_ID(). */
    Long lastInsertedId();

    /** 직접 등록(MANUAL) + 미매칭 항목의 전체 필드 수정. 호출부에서 이미 기존값과 병합해 넘긴다. */
    int updateCashFlowManual(
            @Param("cashFlowId") Long cashFlowId,
            @Param("bankName") String bankName,
            @Param("tradedAt") LocalDateTime tradedAt,
            @Param("type") String type,
            @Param("amount") BigDecimal amount,
            @Param("depositorName") String depositorName,
            @Param("memo") String memo);

    /** CSV/API 출처이거나 이미 매칭된 항목의 메모만 수정. */
    int updateCashFlowMemo(@Param("cashFlowId") Long cashFlowId, @Param("memo") String memo);

    /** 소프트 삭제(배치) — 매칭 안 된 것만 호출부에서 걸러서 넘긴다. */
    int softDeleteBatch(@Param("cashFlowIds") List<Long> cashFlowIds);

    /** 연결 제외 처리(배치) — 매칭돼서 제외 못 하는 것만 호출부에서 걸러서 넘긴다. */
    int updateExcludedBatch(@Param("cashFlowIds") List<Long> cashFlowIds, @Param("isExcluded") boolean isExcluded);
}
