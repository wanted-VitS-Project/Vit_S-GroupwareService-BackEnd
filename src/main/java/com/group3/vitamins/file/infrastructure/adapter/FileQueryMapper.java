package com.group3.vitamins.file.infrastructure.adapter;

import com.group3.vitamins.file.application.result.BlockFileProjection;
import com.group3.vitamins.file.application.result.FileVersionProjection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 파일 화면용 조회 (MyBatis · 조회 전용). SQL 은 XML 에 둔다. */
@Mapper
public interface FileQueryMapper {

    boolean existsActiveNameInBlock(@Param("blockId") Long blockId, @Param("name") String name);

    Long findBlockIdByFileId(@Param("fileId") Long fileId);

    List<FileVersionProjection> findCompletedVersions(@Param("fileId") Long fileId);

    int findMaxCompletedVersionNo(@Param("fileId") Long fileId);

    List<BlockFileProjection> findBlockFiles(@Param("blockId") Long blockId, @Param("deleted") boolean deleted);
}
