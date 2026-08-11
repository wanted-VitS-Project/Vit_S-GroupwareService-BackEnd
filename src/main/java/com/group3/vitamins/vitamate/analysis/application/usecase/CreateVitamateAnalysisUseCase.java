package com.group3.vitamins.vitamate.analysis.application.usecase;

import com.group3.vitamins.vitamate.analysis.application.command.CreateVitamateAnalysisCommand;
import com.group3.vitamins.vitamate.analysis.application.result.CreateVitamateAnalysisResult;

// 비타메이트 분석 요청 생성 유스케이스
public interface CreateVitamateAnalysisUseCase {

    CreateVitamateAnalysisResult handle(CreateVitamateAnalysisCommand command);
}