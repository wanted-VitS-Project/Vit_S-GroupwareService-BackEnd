package com.group3.vitamins.settlement.infrastructure.blockdetail;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/** 블록 조회용 settlement_block 배치 조회. 쓰기는 JPA(SettlementRepository) 가 담당한다. */
@Mapper
public interface SettlementDetailMapper {

    List<SettlementDetailRow> findBySettleIds(@Param("settleIds") Collection<Long> settleIds);

    /**
     * 같은 프로젝트·같은 타입의 다른 정산 블록이 이미 갖고 있는 총 예정 금액(total_amount)을 찾는다.
     * 여러 회차가 있어도 전부 같은 값이어야 하므로 하나만 가져오면 된다. 없으면(아직 아무 회차도
     * 값을 안 채웠으면) null — 이 경우 지금 요청이 그 프로젝트의 첫 기준값이 된다.
     */
    Long findEstablishedTotalAmount(@Param("settleId") Long settleId, @Param("type") String type);

    /**
     * 정산 항목 수정 화면 진입 시(GET .../items?type=...) 추천 회차 번호·추천 총 금액을 계산하기 위한 조회.
     * "타입 변경 탭"에서 호출하는 것으로 확정되어 타입을 항상 알고 있으므로, 같은 프로젝트·같은 타입으로 좁힌다.
     */
    SettlementRecommendationRow findRecommendation(@Param("settleId") Long settleId, @Param("type") String type);
}
