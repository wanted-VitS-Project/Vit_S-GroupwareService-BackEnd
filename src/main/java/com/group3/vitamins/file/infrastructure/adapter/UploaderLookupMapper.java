package com.group3.vitamins.file.infrastructure.adapter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 업로더 스냅샷 조회 (MyBatis · 조회 전용). SQL 은 XML 에 둔다(팀 컨벤션 — @Select 금지). */
@Mapper
public interface UploaderLookupMapper {

    UploaderRow findByUserId(@Param("userId") String userId);
}
