package com.group3.vitamins.finance.infrastructure.taxinvoice;

import com.group3.vitamins.finance.infrastructure.taxinvoice.csv.ParsedTaxInvoiceRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 세금계산서 CSV 업로드·매칭/매칭 해제 전용. 조회는 TaxInvoiceMapper 소관, 이쪽은 쓰기만 담당한다. */
@Mapper
public interface TaxInvoiceCommandMapper {

    /**
     * candidates 중 이미 DB에 존재하는 approval_no만 골라 돌려준다. ⚠️ company_id로 좁히지 않는다 —
     * uk_tax_invoice_approval_no가 회사 스코프 없이 테이블 전체에서 유일해야 하는 값이라서(국세청
     * 승인번호는 전국 유일값, 2026-08-09 확정), 다른 회사가 먼저 등록한 승인번호도 중복으로 잡아야
     * DB 유니크 제약과 판정 결과가 일치한다.
     */
    List<String> findExistingApprovalNos(@Param("candidates") List<String> candidates);

    /** 중복 제거가 끝난 행만 일괄 INSERT한다. type은 업로드 요청 전체에 하나(라디오 버튼)라 행마다 따로 안 받는다. */
    int insertAll(
            @Param("companyId") Long companyId,
            @Param("type") String type,
            @Param("rows") List<ParsedTaxInvoiceRow> rows);

    /** tax_invoice 쪽 연결 정보 저장. */
    int updateTaxInvoiceMatch(
            @Param("taxId") Long taxId,
            @Param("settleId") Long settleId,
            @Param("linkedBy") String linkedBy,
            @Param("linkedAt") LocalDateTime linkedAt);

    /** 정산 블록 쪽 결과 반영 — status(PARTIAL/COMPLETED)·actual_amount·actual_date. */
    int updateSettlementBlockMatchResult(
            @Param("settleId") Long settleId,
            @Param("status") String status,
            @Param("actualAmount") BigDecimal actualAmount,
            @Param("actualDate") LocalDateTime actualDate);

    /** tax_invoice 쪽 연결 정보 해제. */
    int clearTaxInvoiceMatch(@Param("taxId") Long taxId);

    /** 정산 블록을 PENDING으로 되돌리고 실적값을 비운다. */
    int resetSettlementBlockMatch(@Param("settleId") Long settleId);
}
