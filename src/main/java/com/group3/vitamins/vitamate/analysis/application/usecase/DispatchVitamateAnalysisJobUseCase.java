package com.group3.vitamins.vitamate.analysis.application.usecase;

import com.group3.vitamins.vitamate.analysis.application.command.DispatchVitamateAnalysisJobCommand;

// 비타메이트 분석 작업을 비동기 큐로 발행하는 유스케이스
public interface DispatchVitamateAnalysisJobUseCase {

    void handle(DispatchVitamateAnalysisJobCommand command);
}
