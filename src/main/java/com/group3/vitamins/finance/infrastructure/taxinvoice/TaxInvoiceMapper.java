package com.group3.vitamins.finance.infrastructure.taxinvoice;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/** 세금계산서(tax_invoice) 조회 전용. 쓰기(업로드·매칭)는 아직 이 도메인에 없다(cash_flow와 동일한 순서로 예정). */
@Mapper
public interface TaxInvoiceMapper {

    List<TaxInvoiceRow> findTaxInvoices(
            @Param("companyId") Long companyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("unlinked") Boolean unlinked,
            @Param("projectId") Long projectId,
            @Param("keyword") String keyword,
            @Param("sort") String sort,
            @Param("size") int size,
            @Param("offset") int offset);

    /** 위 목록과 같은 필터의 전체 개수(페이징용) — sort/size/offset은 개수와 무관해 안 받는다. */
    long countTaxInvoices(
            @Param("companyId") Long companyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("unlinked") Boolean unlinked,
            @Param("projectId") Long projectId,
            @Param("keyword") String keyword);

    /** 필터 옵션용 — tax_invoice가 하나라도 연결된 정산 블록을 가진 프로젝트만. */
    List<TaxInvoiceFilterProjectRow> findFilterProjects(@Param("companyId") Long companyId);
}
