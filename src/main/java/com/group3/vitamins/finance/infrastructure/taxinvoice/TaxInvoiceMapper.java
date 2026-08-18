package com.group3.vitamins.finance.infrastructure.taxinvoice;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 세금계산서(tax_invoice) 조회 전용. 쓰기(업로드·매칭 UPDATE)는 TaxInvoiceCommandMapper 소관이다. */
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

    /**
     * 단건 조회(2026-08-18 신설) — findTaxInvoices와 조인·컬럼이 완전히 동일하고 tax_id로만 좁힌다.
     * 존재하지 않거나(삭제 포함) 다른 회사 소속이면 null.
     */
    TaxInvoiceRow findTaxInvoiceById(@Param("taxId") Long taxId, @Param("companyId") Long companyId);

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

    /** 매칭 추천 조회의 기준값 — 존재하지 않거나(삭제 포함) 다른 회사 소속이면 null. */
    TaxInvoiceBasicRow findBasicById(@Param("taxId") Long taxId, @Param("companyId") Long companyId);

    /**
     * 매칭 추천 후보 — 같은 타입의 PENDING 정산 블록 중 금액/세액/상호명/발행일 중 하나라도 걸리는 것만,
     * 매칭 개수 많은 순으로 최대 5건.
     */
    List<TaxInvoiceMatchCandidateRow> findMatchCandidates(
            @Param("type") String type,
            @Param("totalAmount") BigDecimal totalAmount,
            @Param("taxAmount") BigDecimal taxAmount,
            @Param("issuedNo") LocalDate issuedNo,
            @Param("buyerName") String buyerName,
            @Param("companyId") Long companyId);

    /** 매칭/매칭 해제 검증용 — 존재하지 않거나(삭제 포함) 다른 회사 소속이면 null. */
    TaxInvoiceMatchLookupRow findMatchLookup(@Param("taxId") Long taxId, @Param("companyId") Long companyId);

    /** 매칭 대상 정산 블록 검증용 — 존재하지 않거나(정산 블록·공용 블록·다른 회사 소속) 이면 null. */
    SettlementBlockMatchRow findSettlementBlockForMatch(
            @Param("settleId") Long settleId, @Param("companyId") Long companyId);

    /**
     * 매칭 직전 정산 블록 행에 쓰기 잠금을 건다 — 이후 {@link #findLinkedTaxInvoiceId}까지가 임계 구역이다.
     * 반환값은 쓰지 않는다(잠금이 목적).
     */
    Long lockSettlementBlockForUpdate(@Param("settleId") Long settleId);

    /**
     * 이 정산 블록에 이미 연결된 세금계산서의 ID — 없으면 null.
     * ⚠️ 반드시 {@link #lockSettlementBlockForUpdate} 뒤에 호출하고, 쿼리 자체도 잠금 읽기여야 한다.
     */
    Long findLinkedTaxInvoiceId(@Param("settleId") Long settleId);

    /** 매칭 UPDATE 직후 응답 조립용. */
    TaxInvoiceMatchResultRow findMatchResultById(@Param("taxId") Long taxId, @Param("companyId") Long companyId);

    /** 메모 수정 직후 응답 조립용 — 존재하지 않거나 다른 회사 소속이면 null. */
    TaxInvoiceMemoRow findMemoById(@Param("taxId") Long taxId, @Param("companyId") Long companyId);

    /**
     * 삭제·연결 제외 처리 대상 확인용 — 요청한 ID 중 실재하는(삭제 안 됨·같은 회사) 것만 돌려준다.
     * 결과에 없는 ID는 "존재하지 않음"으로 처리한다(cash_flow의 findDeleteCandidates와 동일).
     */
    List<TaxInvoiceDeleteCandidateRow> findDeleteCandidates(
            @Param("taxIds") List<Long> taxIds, @Param("companyId") Long companyId);
}
