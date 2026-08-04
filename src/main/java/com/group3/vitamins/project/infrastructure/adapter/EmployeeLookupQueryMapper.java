package com.group3.vitamins.project.infrastructure.adapter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

/** {@code employee} 테이블 직접 조회 — 사번으로 이름만 필요하다 ({@code AuthQueryMapper} 선례). */
@Mapper
public interface EmployeeLookupQueryMapper {

    @Select("SELECT name FROM employee WHERE user_id = #{userId}")
    Optional<String> findNameByUserId(@Param("userId") String userId);
}