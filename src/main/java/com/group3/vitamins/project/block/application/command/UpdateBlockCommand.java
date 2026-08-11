package com.group3.vitamins.project.block.application.command;

/**
 * 블록 제목·담당자 수정. xxxProvided 로 "생략" 과 "null 명시" 를 구분한다 —
 * 담당자 해제를 표현할 수단이 null 명시뿐이다 (BLK-012).
 *
 * @param version   조회에서 받은 버전. 이 값이 DB 와 다르면 409 다
 * @param overwrite true 면 충돌을 무시하고 DB 현재 버전을 기대값으로 써서 덮어쓴다
 */
public record UpdateBlockCommand(
        Long blockId,
        boolean titleProvided,
        String title,
        boolean ownerProvided,
        String owner,
        int version,
        boolean overwrite,
        String requesterUserId,
        String role
) {
}
