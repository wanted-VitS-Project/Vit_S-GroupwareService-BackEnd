package com.group3.vitamins.project.block.application.command;

/** 블록 이동 (BLK-014). 같은 프로젝트의 다른 스텝으로만 옮길 수 있다. */
public record MoveBlockCommand(
        Long blockId,
        Long stepId,
        String requesterUserId,
        String role
) {
}
