package com.group3.vitamins.settlement.infrastructure.blockdetail;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 정산 블록 생성 시점에 스탬핑할 project_id 를 block->step 으로 한 번만 찾아오는 조회.
 *
 * <p>이 조인이 필요한 마지막 지점이다 — 조회 경로는 {@code settlement_block.project_id} 를
 * 직접 읽으므로 더 이상 block/step 을 타지 않는다({@code V20260816170100}).
 */
@Mapper
public interface SettlementProjectLookupMapper {

    Long findProjectIdByBlockId(@Param("blockId") Long blockId);
}
