package com.group3.vitamins.finance.infrastructure.status;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 재무 관리 요약 위젯용 집계 조회. cash_flow·tax_invoice·settlement_block(settlement 도메인 소유)과
 * project(project 도메인 소유) 테이블을 직접 조회한다 — 순수 집계 읽기라 재사용할 기존 서비스 로직이
 * 없어 어댑터가 SQL로 직접 계산한다(§2-2, "상대에게 재사용할 로직이 없으면 직접 SQL로 쓴다"와 동일하게
 * 조회에도 적용). 쓰기는 없다.
 */
@Mapper
public interface FinanceSummaryMapper {

    FinanceSummaryRow findSummary(@Param("companyId") Long companyId);
}
