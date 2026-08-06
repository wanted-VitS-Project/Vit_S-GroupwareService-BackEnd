package com.group3.vitamins.vitamate.analysis.infrastructure.persistence.mapper;

import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.row.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// 비타메이트 블록 권한과 파일 버전 검증 SQL을 호출하는 Mapper
@Mapper
public interface VitamateAnalysisMapper {

    // AI 블록, 비타메이트 블록, 스텝, 프로젝트 권한을 함께 조회한다.
    VitamateBlockContextRow findAccessibleVitamateBlock(
            @Param("blockId") Long blockId,
            @Param("userId") String userId
    );

    // 선택된 파일 버전 중 해당 프로젝트에 속하고 업로드 완료된 건수를 센다.
    int countCompletedFileVersionsInProject(
            @Param("projectId") Long projectId,
            @Param("fileVersionIds") List<Long> fileVersionIds
    );

    // 요청자가 접근할 수 있는 분석 상세 본문을 조회한다.
    VitamateAnalysisRow findAccessibleAnalysis(
            @Param("analysisId") Long analysisId,
            @Param("userId") String userId
    );

    // 분석 요청 당시 선택된 문서 목록을 조회한다.
    List<VitamateAnalysisDocumentRow> findAnalysisDocuments(@Param("analysisId") Long analysisId);

    // 분석 결과의 근거 citation 목록을 순서대로 조회한다.
    List<VitamateAnalysisCitationRow> findAnalysisCitations(@Param("analysisId") Long analysisId);

    // Python worker가 처리할 수 있는 PROCESSING 분석 작업 기본 정보를 조회한다.
    VitamateAnalysisJobRow findProcessingAnalysisJob(
            @Param("analysisId") Long analysisId,
            @Param("attemptId") String attemptId
    );

    // Python worker 분석 작업의 선택 문서 목록을 조회한다.
    List<VitamateAnalysisJobDocumentRow> findAnalysisJobDocuments(@Param("analysisId") Long analysisId);

    // Python worker 분석 작업의 문서별 후보 청크 목록을 조회한다.
    List<VitamateAnalysisJobChunkRow> findAnalysisJobChunks(@Param("analysisId") Long analysisId);

    // 비타메이트 블록 ID를 기준으로 분석 실행 이력 목록을 지정한 개수까지만 조회합니다.
    List<VitamateAnalysisHistoryRow> findBlockAnalysisHistories(
            @Param("vitamateBlockId") Long vitamateBlockId,
            @Param("limit") int limit
    );
}

