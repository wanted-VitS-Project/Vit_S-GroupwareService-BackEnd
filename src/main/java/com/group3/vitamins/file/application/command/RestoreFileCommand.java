package com.group3.vitamins.file.application.command;

/** 휴지통 복구 명령(§6). 블록이 삭제됐어도 복구된다(그때는 blockId=null·blockDeleted=true). */
public record RestoreFileCommand(
        Long fileId,
        String requesterUserId,
        String role
) {
}
