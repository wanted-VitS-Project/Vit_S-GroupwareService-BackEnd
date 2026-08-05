package com.group3.vitamins.vitamate.infrastructure.blockdetail;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

// AI 블록 상세 조회 SQL을 호출하는 MyBatis Mapper
@Mapper
public interface VitamateDetailMapper {

    // typeId 목록으로 삭제되지 않은 AI 블록 상세 행을 배치 조회한다.
    List<VitamateDetailRow> findByVitamateBlockIds(@Param("vitamateBlockIds") Collection<Long> vitamateBlockIds);
}
