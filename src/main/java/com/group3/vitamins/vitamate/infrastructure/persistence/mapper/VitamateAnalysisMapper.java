package com.group3.vitamins.vitamate.infrastructure.persistence.mapper;

import com.group3.vitamins.vitamate.infrastructure.persistence.row.VitamateBlockContextRow;
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
}
