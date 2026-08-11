package com.group3.vitamins.vitamate.filecleanup.application.usecase;

import com.group3.vitamins.vitamate.filecleanup.application.command.HandleVitamateCleanupCallbackCommand;
import com.group3.vitamins.vitamate.filecleanup.application.result.VitamateCleanupCallbackResult;

// Python worker의 ChromaDB 정리 결과를 처리하는 유스케이스입니다.
public interface HandleVitamateCleanupCallbackUseCase {

    VitamateCleanupCallbackResult handle(
            HandleVitamateCleanupCallbackCommand command
    );
}