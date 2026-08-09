package com.group3.vitamins.settlement.infrastructure.adapter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** page_permission 테이블 조회 전용. 쓰기(부여/회수)는 이 도메인 소관이 아니다. */
@Mapper
public interface PagePermissionMapper {

    boolean existsGrant(@Param("pageCode") String pageCode, @Param("userId") String userId);
}
