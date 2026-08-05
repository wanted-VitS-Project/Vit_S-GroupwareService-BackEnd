package com.group3.vitamins.file.application.command;

/** 휴지통 이동 명령(§5). 저장소 객체는 지우지 않고 삭제 시각만 기록한다. */
public record TrashFileCommand(
        Long fileId,
        String requesterUserId,
        String role
) {
}
