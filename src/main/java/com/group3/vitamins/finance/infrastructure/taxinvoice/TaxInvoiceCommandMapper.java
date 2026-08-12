package com.group3.vitamins.finance.infrastructure.taxinvoice;

import com.group3.vitamins.finance.infrastructure.taxinvoice.csv.ParsedTaxInvoiceRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 세금계산서 CSV 업로드 전용 — 쓰기(등록)만 담당한다. 매칭/삭제 등은 아직 이 도메인에 없다. */
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
}
