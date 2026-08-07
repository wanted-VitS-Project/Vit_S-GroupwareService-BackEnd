package com.group3.vitamins.vitamate.analysis.application.usecase;

import com.group3.vitamins.vitamate.analysis.application.query.GetVitamateBlockAnalysisHistoryQuery;
import com.group3.vitamins.vitamate.analysis.application.result.VitamateAnalysisHistoryResult;

// 비타메이트 블록별 분석 실행 이력을 조회하는 유스케이스입니다.
public interface GetVitamateBlockAnalysisHistoryUseCase {

    VitamateAnalysisHistoryResult handle(GetVitamateBlockAnalysisHistoryQuery query);
}