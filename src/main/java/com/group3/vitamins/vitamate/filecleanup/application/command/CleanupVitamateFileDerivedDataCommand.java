package com.group3.vitamins.vitamate.filecleanup.application.command;

// 파일 영구삭제 시 비타메이트 파생 데이터를 정리하기 위한 명령
public record CleanupVitamateFileDerivedDataCommand(
        Long fileId
) {
}