package com.group3.vitamins.vitamate.infrastructure.persistence.mapper;

import com.group3.vitamins.vitamate.infrastructure.persistence.row.VitamateBlockContextRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// 비타메이트 블록 권한과 파일 버전 검증 SQL을 호출하는 Mapper
@Mapper
public interface VitamateAnalysisMapper {

    VitamateBlockContextRow findAccessibleVitamateBlock(
            @Param("blockId") Long blockId,
            @Param("userId") String userId
    );

    int countCompletedFileVersionsInProject(
            @Param("projectId") Long projectId,
            @Param("fileVersionIds") List<Long> fileVersionIds
    );
}