package com.group3.vitamins.vitamate.analysis.application.usecase;

import com.group3.vitamins.vitamate.analysis.application.query.GetVitamateAnalysisJobQuery;
import com.group3.vitamins.vitamate.analysis.application.result.VitamateAnalysisJobDetailResult;

// Python worker가 분석 실행 전에 필요한 작업 입력을 조회하는 유스케이스
public interface GetVitamateAnalysisJobUseCase {
    VitamateAnalysisJobDetailResult handle(GetVitamateAnalysisJobQuery query);
}
