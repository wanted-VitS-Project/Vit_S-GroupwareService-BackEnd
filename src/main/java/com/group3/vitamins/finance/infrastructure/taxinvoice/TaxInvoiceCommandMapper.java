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
            @Param("linkedAt") LocalDateTime linkedAt,
            @Param("companyId") Long companyId);

    /**
     * 정산 블록을 WAITING(정산 대기)으로 올린다 — PENDING 이었을 때만.
     * 이미 PARTIAL/COMPLETED(입출금이 붙은 블록)면 0행이 정상이다(그 상태를 유지해야 한다).
     */
    int markSettlementBlockWaiting(@Param("settleId") Long settleId);

    /** tax_invoice 쪽 연결 정보 해제. */
    int clearTaxInvoiceMatch(@Param("taxId") Long taxId, @Param("companyId") Long companyId);

    /**
     * 정산 블록을 PENDING 으로 되돌린다 — WAITING(세금계산서만 붙어 있던 상태)이었을 때만.
     * 입출금이 아직 붙어 있으면 PARTIAL/COMPLETED 라 0행이 되고, 그 상태·실적값이 그대로 유지된다.
     */
    int resetSettlementBlockFromWaiting(@Param("settleId") Long settleId);

    /** 메모만 수정한다 — 세금계산서 원본 값(승인번호·금액·사업자번호 등)은 고칠 수 없다. */
    int updateTaxInvoiceMemo(
            @Param("taxId") Long taxId, @Param("memo") String memo, @Param("companyId") Long companyId);

    /** 소프트 삭제(배치) — 매칭된 항목은 지우지 않는다(조건을 UPDATE 문에 걸어 확인~삭제 사이의 틈을 없앤다). */
    int softDeleteBatch(@Param("taxIds") List<Long> taxIds, @Param("companyId") Long companyId);

    /** 연결 제외/포함 배치 처리. */
    int updateExcludedBatch(
            @Param("taxIds") List<Long> taxIds,
            @Param("isExcluded") Boolean isExcluded,
            @Param("companyId") Long companyId);
}
