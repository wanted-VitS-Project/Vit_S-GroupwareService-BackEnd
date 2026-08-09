package com.group3.vitamins.settlement.infrastructure.blockdetail;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 블록 조회용 settlement_block 배치 조회 — {@code BlockDetailPort} 구현체({@link SettlementBlockDetailAdapter})가
 * 블록 목록 조회 응답을 채울 때 쓴다. 쓰기는 JPA({@code SettlementRepository})가 담당한다.
 *
 * <p>정산 도메인 자기 자신의 비즈니스 로직(SETL-008·추천값·진행률)에 필요한 조회는
 * {@link SettlementSiblingMapper}로 분리돼 있다 — 목적이 다른 두 소비자가 같은 매퍼를 공유하지 않게 한다.
 */
@Mapper
public interface SettlementDetailMapper {

    List<SettlementDetailRow> findBySettleIds(@Param("settleIds") Collection<Long> settleIds);
}
