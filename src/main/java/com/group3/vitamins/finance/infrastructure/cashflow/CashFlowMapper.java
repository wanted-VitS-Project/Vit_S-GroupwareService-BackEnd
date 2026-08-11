package com.group3.vitamins.finance.infrastructure.cashflow;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 입출금 내역(cash_flow) 조회 전용. 쓰기(업로드·매칭)는 아직 이 도메인에 없다. */
@Mapper
public interface CashFlowMapper {

    List<CashFlowRow> findCashFlows(
            @Param("companyId") Long companyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("unlinked") Boolean unlinked,
            @Param("projectId") Long projectId,
            @Param("keyword") String keyword);

    /** 필터 옵션용 — cash_flow가 하나라도 연결된 정산 블록을 가진 프로젝트만. */
    List<CashFlowFilterProjectRow> findFilterProjects(@Param("companyId") Long companyId);

    /** 매칭 추천 조회의 기준값 — 존재하지 않거나(삭제 포함) 다른 회사 소속이면 null. */
    CashFlowBasicRow findBasicById(@Param("cashFlowId") Long cashFlowId, @Param("companyId") Long companyId);

    /**
     * 매칭 추천 후보 — 같은 타입의 PENDING 정산 블록 중 금액/일자/거래처명 중 하나라도 걸리는 것만,
     * 매칭 개수 많은 순으로 최대 5건.
     */
    List<MatchCandidateRow> findMatchCandidates(
            @Param("type") String type,
            @Param("amount") BigDecimal amount,
            @Param("tradedDate") LocalDate tradedDate,
            @Param("depositorName") String depositorName,
            @Param("companyId") Long companyId);

    /** 매칭/매칭 해제 검증용 — 존재하지 않거나(삭제 포함) 다른 회사 소속이면 null. */
    CashFlowMatchLookupRow findMatchLookup(@Param("cashFlowId") Long cashFlowId, @Param("companyId") Long companyId);

    /** 매칭 대상 정산 블록 검증용 — 존재하지 않거나(정산 블록·공용 블록·다른 회사 소속) 이면 null. */
    SettlementBlockMatchRow findSettlementBlockForMatch(
            @Param("settleId") Long settleId, @Param("companyId") Long companyId);

    /** 매칭 UPDATE 직후 응답 조립용. */
    CashFlowMatchResultRow findMatchResultById(@Param("cashFlowId") Long cashFlowId);

    /** 등록/수정 응답 조립 + 수정 병합용 — 존재하지 않거나(삭제 포함) 다른 회사 소속이면 null. */
    CashFlowDetailRow findDetailById(@Param("cashFlowId") Long cashFlowId, @Param("companyId") Long companyId);

    /** 등록 시 중복 판정 — 같은 회사·은행·거래일시·금액(잔액은 항상 null인 직접 등록/API 항목 기준)이 이미 있는지. */
    boolean existsDuplicate(
            @Param("companyId") Long companyId,
            @Param("bankName") String bankName,
            @Param("tradedAt") LocalDateTime tradedAt,
            @Param("amount") BigDecimal amount);

    /** 수정 시 중복 판정 — 자기 자신은 제외하고 같은 조합이 있는지. */
    boolean existsDuplicateExcluding(
            @Param("cashFlowId") Long cashFlowId,
            @Param("companyId") Long companyId,
            @Param("bankName") String bankName,
            @Param("tradedAt") LocalDateTime tradedAt,
            @Param("amount") BigDecimal amount);

    /** 배치 삭제 대상 판정용 — 요청받은 ID 중 존재하는(삭제 안 된) 것만 돌아온다. */
    List<CashFlowDeleteCandidateRow> findDeleteCandidates(
            @Param("cashFlowIds") List<Long> cashFlowIds, @Param("companyId") Long companyId);
}
