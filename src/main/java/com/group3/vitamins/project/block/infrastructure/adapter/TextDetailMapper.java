package com.group3.vitamins.project.block.infrastructure.adapter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface TextDetailMapper {

    int insertEmpty(@Param("blockId") Long blockId);

    Long findTxtIdByBlockId(@Param("blockId") Long blockId);

    List<TextDetailRow> findByTxtIds(@Param("txtIds") Collection<Long> txtIds);
}