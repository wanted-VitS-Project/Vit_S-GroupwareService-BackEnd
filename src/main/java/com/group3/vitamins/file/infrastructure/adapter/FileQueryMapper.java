package com.group3.vitamins.file.infrastructure.adapter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 파일 화면용 조회 (MyBatis · 조회 전용). SQL 은 XML 에 둔다. */
@Mapper
public interface FileQueryMapper {

    boolean existsActiveNameInBlock(@Param("blockId") Long blockId, @Param("name") String name);
}
