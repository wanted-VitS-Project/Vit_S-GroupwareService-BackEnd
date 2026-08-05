package com.group3.vitamins.vitamate.application.usecase;

import com.group3.vitamins.vitamate.application.command.HandleVitamateAnalysisCallbackCommand;
import com.group3.vitamins.vitamate.application.result.VitamateAnalysisCallbackResult;

// Python 분석 결과 callback을 처리하는 유스케이스
public interface HandleVitamateAnalysisCallbackUseCase {
    VitamateAnalysisCallbackResult handle(HandleVitamateAnalysisCallbackCommand command);
}