package com.group3.vitamins.project.block.application.command;

/**
 * 블록 제목·담당자 수정. xxxProvided 로 "생략" 과 "null 명시" 를 구분한다 —
 * 담당자 해제를 표현할 수단이 null 명시뿐이다 (BLK-012).
 */
public record UpdateBlockCommand(
        Long blockId,
        boolean titleProvided,
        String title,
        boolean ownerProvided,
        String owner,
        String requesterUserId,
        String role
) {
}