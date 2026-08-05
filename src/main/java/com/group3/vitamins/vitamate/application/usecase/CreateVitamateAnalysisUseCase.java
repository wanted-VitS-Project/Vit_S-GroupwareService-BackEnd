package com.group3.vitamins.vitamate.application.usecase;

import com.group3.vitamins.vitamate.application.command.CreateVitamateAnalysisCommand;
import com.group3.vitamins.vitamate.application.result.CreateVitamateAnalysisResult;

// 비타메이트 분석 요청 생성 유스케이스
public interface CreateVitamateAnalysisUseCase {

    CreateVitamateAnalysisResult handle(CreateVitamateAnalysisCommand command);
}