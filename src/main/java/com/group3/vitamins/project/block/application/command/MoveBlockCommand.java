package com.group3.vitamins.project.block.application.command;

/**
 * 블록 이동 (BLK-014). 같은 프로젝트의 다른 스텝으로만 옮길 수 있다.
 *
 * @param version   조회에서 받은 버전. 이 값이 DB 와 다르면 409 다
 * @param overwrite true 면 충돌을 무시하고 DB 현재 버전을 기대값으로 써서 덮어쓴다
 */
public record MoveBlockCommand(
        Long blockId,
        Long stepId,
        int version,
        boolean overwrite,
        String requesterUserId,
        String role
) {
}
