package com.group3.vitamins.project.infrastructure.adapter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** {@code employee} 테이블 직접 조회 — 사번으로 이름만 필요하다 ({@code AuthQueryMapper} 선례). */
@Mapper
public interface EmployeeLookupQueryMapper {

    @Select("SELECT name FROM employee WHERE user_id = #{userId}")
    Optional<String> findNameByUserId(@Param("userId") String userId);

    @Select("""
            <script>
            SELECT user_id AS userId, name
              FROM employee
             WHERE user_id IN
            <foreach item="id" collection="userIds" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    List<EmployeeNameRow> findNamesByUserIds(@Param("userIds") Collection<String> userIds);
}