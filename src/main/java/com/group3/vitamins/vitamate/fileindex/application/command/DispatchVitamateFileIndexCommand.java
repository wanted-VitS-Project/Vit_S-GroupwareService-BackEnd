package com.group3.vitamins.vitamate.fileindex.application.command;

// 비타메이트 파일 인덱싱 작업 큐 발행 요청 값
public record DispatchVitamateFileIndexCommand(
        Long fileVersionId
) {
}