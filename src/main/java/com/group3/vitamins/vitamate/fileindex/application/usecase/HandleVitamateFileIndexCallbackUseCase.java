package com.group3.vitamins.vitamate.fileindex.application.usecase;

import com.group3.vitamins.vitamate.fileindex.application.command.HandleVitamateFileIndexCallbackCommand;
import com.group3.vitamins.vitamate.fileindex.application.result.VitamateFileIndexCallbackResult;

// 파일 인덱싱 상태 callback을 처리하는 유스케이스
public interface HandleVitamateFileIndexCallbackUseCase {

    VitamateFileIndexCallbackResult handle(HandleVitamateFileIndexCallbackCommand command);
}