package com.group3.vitamins.finance.infrastructure.adapter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * page_permission 테이블 조회 전용. 쓰기(부여/회수)는 이 도메인 소관이 아니다.
 *
 * <p>settlement 도메인의 동명 매퍼(PagePermissionMapper)와 클래스명이 겹치면 컴포넌트 스캔 시
 * 빈 이름 충돌(ConflictingBeanDefinitionException)이 나서 finance 접두어를 붙였다.
 */
@Mapper
public interface FinancePagePermissionMapper {

    boolean existsGrant(@Param("pageCode") String pageCode, @Param("userId") String userId);

    /** {@code permission = 'EDITOR'} 인 행이 있는지만 본다(CSV 업로드 등 쓰기 API용). */
    boolean existsEditGrant(@Param("pageCode") String pageCode, @Param("userId") String userId);
}
