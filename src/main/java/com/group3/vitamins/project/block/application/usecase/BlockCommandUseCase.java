package com.group3.vitamins.project.block.application.usecase;

import com.group3.vitamins.project.block.application.command.CreateBlockCommand;
import com.group3.vitamins.project.block.application.result.BlockResult;

public interface BlockCommandUseCase {

    /** 블록과 타입별 상세 빈 행을 한 트랜잭션에서 만든다. */
    BlockResult createBlock(CreateBlockCommand command);
}