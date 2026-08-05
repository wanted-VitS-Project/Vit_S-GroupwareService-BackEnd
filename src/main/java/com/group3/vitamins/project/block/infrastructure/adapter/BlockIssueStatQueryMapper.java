package com.group3.vitamins.project.block.infrastructure.adapter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface BlockIssueStatQueryMapper {

    List<BlockIssueStatRow> countByBlockIds(@Param("blockIds") Collection<Long> blockIds);
}