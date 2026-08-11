package com.group3.vitamins.file.infrastructure.adapter;

import com.group3.vitamins.file.application.result.BlockFileProjection;
import com.group3.vitamins.file.application.result.FileVersionProjection;
import com.group3.vitamins.file.application.result.ProjectFileProjection;
import com.group3.vitamins.file.application.result.ProjectTrashFileProjection;
import com.group3.vitamins.file.application.result.ProjectFileVersionProjection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 파일 화면용 조회 (MyBatis · 조회 전용). SQL 은 XML 에 둔다. */
@Mapper
public interface FileQueryMapper {

    boolean existsActiveNameInBlock(@Param("blockId") Long blockId, @Param("name") String name);

    Long findBlockIdByFileId(@Param("fileId") Long fileId);

    Long findStepIdByFileIdIncludingDeletedBlock(@Param("fileId") Long fileId);

    List<FileVersionProjection> findCompletedVersions(@Param("fileId") Long fileId);

    int findMaxCompletedVersionNo(@Param("fileId") Long fileId);

    List<BlockFileProjection> findBlockFiles(@Param("blockId") Long blockId, @Param("deleted") boolean deleted);

    List<ProjectFileVersionProjection> findProjectFileVersions(@Param("projectId") Long projectId);

    List<ProjectFileProjection> findProjectFiles(@Param("projectId") Long projectId);

    List<ProjectTrashFileProjection> findProjectTrashFiles(@Param("projectId") Long projectId);
}
