package com.group3.vitamins.settlement.infrastructure.status;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/** 재무팀 정산현황 화면용 집계 조회. 쓰기는 없다(정산 항목 자체는 SettlementRepository/PATCH 소관). */
@Mapper
public interface SettlementStatusMapper {

    /** 정산현황에 등장하는(=활성 정산 블록이 하나라도 있는 프로젝트의) 발주처명 목록. 중복 없이 오름차순. */
    List<String> findDistinctClientNames(@Param("companyId") Long companyId);

    /**
     * 정산 현황 프로젝트 조회 — 재무팀이 보는 전체 프로젝트 단위 목록(페이지 단위).
     * startDate/endDate 는 nextPlannedDate(프로젝트별 다음 정산 예정일) 기준으로 거른다.
     */
    List<SettlementProjectRow> findProjectSettlements(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("client") String client,
            @Param("includeCompleted") Boolean includeCompleted,
            @Param("sort") String sort,
            @Param("size") int size,
            @Param("offset") int offset,
            @Param("companyId") Long companyId);

    /** 위 목록과 같은 필터 조건의 전체 개수(페이징용) — sort/size/offset은 개수에 영향 없어 안 받는다. */
    long countProjectSettlements(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("client") String client,
            @Param("includeCompleted") Boolean includeCompleted,
            @Param("companyId") Long companyId);

    /** 정산 현황 블록 조회 — 한 프로젝트에 속한 정산 블록 회차별 내역 전체. */
    List<SettlementProjectBlockRow> findProjectSettlementBlocks(
            @Param("projectId") Long projectId, @Param("companyId") Long companyId);

    /** 정산 현황 블록 조회의 404 판정용 — 프로젝트 참여 여부와 무관한 단순 존재 확인(회사 소속까지 포함). */
    boolean existsActiveProject(@Param("projectId") Long projectId, @Param("companyId") Long companyId);
}
