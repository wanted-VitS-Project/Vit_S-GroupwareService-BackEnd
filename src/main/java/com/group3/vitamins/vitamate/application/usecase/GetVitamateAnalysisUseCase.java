package com.group3.vitamins.vitamate.application.usecase;

import com.group3.vitamins.vitamate.application.query.GetVitamateAnalysisQuery;
import com.group3.vitamins.vitamate.application.result.VitamateAnalysisDetailResult;

// 비타메이트 분석 상태와 결과를 조회하는 유스케이스
public interface GetVitamateAnalysisUseCase {

    VitamateAnalysisDetailResult handle(GetVitamateAnalysisQuery query);
}