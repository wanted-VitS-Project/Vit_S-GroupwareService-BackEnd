package com.group3.vitamins.project.block.infrastructure.adapter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ChecklistDetailMapper {

    int insertEmpty(@Param("blockId") Long blockId);

    Long findChkBlockIdByBlockId(@Param("blockId") Long blockId);

    List<ChecklistItemRow> findItemsByChkBlockIds(@Param("chkBlockIds") Collection<Long> chkBlockIds);
}