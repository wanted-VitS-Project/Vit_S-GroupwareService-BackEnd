package com.group3.vitamins.project.block.application.usecase;

import com.group3.vitamins.project.block.application.query.BlockListQuery;
import com.group3.vitamins.project.block.application.result.BlockSummary;

import java.util.List;

public interface BlockQueryUseCase {

    /** 스텝의 블록을 rowIndex → sortOrder 순으로 조회한다. */
    List<BlockSummary> getBlocks(BlockListQuery query);
}