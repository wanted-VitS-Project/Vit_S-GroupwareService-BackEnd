package com.group3.vitamins.vitamate.fileindex.application.usecase;

import com.group3.vitamins.vitamate.fileindex.application.command.DispatchVitamateFileIndexCommand;

// 완료된 파일 버전을 file_index PENDING으로 등록하고 비동기 큐로 발행하는 유스케이스
public interface DispatchVitamateFileIndexUseCase {

    void handle(DispatchVitamateFileIndexCommand command);
}