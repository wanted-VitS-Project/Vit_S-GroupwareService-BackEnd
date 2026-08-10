package com.group3.vitamins.project.block.application.usecase;

import com.group3.vitamins.project.block.application.command.CreateBlockCommand;
import com.group3.vitamins.project.block.application.command.DeleteBlockCommand;
import com.group3.vitamins.project.block.application.command.MoveBlockCommand;
import com.group3.vitamins.project.block.application.command.UpdateBlockCommand;
import com.group3.vitamins.project.block.application.command.UpdateBlockLayoutCommand;
import com.group3.vitamins.project.block.application.result.BlockLayoutResult;
import com.group3.vitamins.project.block.application.result.BlockMoveResult;
import com.group3.vitamins.project.block.application.result.BlockResult;
import com.group3.vitamins.project.block.application.result.BlockUpdateResult;

import java.util.List;

public interface BlockCommandUseCase {

    /** 블록과 타입별 상세 빈 행을 한 트랜잭션에서 만든다. */
    BlockResult createBlock(CreateBlockCommand command);

    /** 제목·담당자만 바꾼다. 배치·타입은 이 경로로 바뀌지 않는다. */
    BlockUpdateResult updateBlock(UpdateBlockCommand command);

    /** 드래그 결과를 스텝 단위로 일괄 반영한다. */
    List<BlockLayoutResult> updateLayout(UpdateBlockLayoutCommand command);

    /**
     * 같은 프로젝트의 다른 스텝으로 옮긴다 (BLK-014).
     * issue_block 연결은 끊긴다 — 블록과 이슈는 같은 스텝이어야 한다 (BLK-009 · INV-06).
     */
    BlockMoveResult moveBlock(MoveBlockCommand command);

    /** 블록과 상세 행을 같은 트랜잭션에서 논리 삭제한다. ⛔ 삭제 잠금은 폐기됐다 (2026-08-09). */
    void deleteBlock(DeleteBlockCommand command);
}