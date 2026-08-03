package com.group3.vitamins.auth.infrastructure.persistence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

/**
 * 인증 화면용 조회. <b>MyBatis 를 쓰는 이유는 애그리거트 4개를 가로지르기 때문</b>이다
 * (계정 · 사원 · 부서 · 직급). JPA 로 짜면 연관관계를 타느라 쿼리가 여러 번 나간다.
 *
 * <p>쓰기는 JPA({@code AccountEntity})가 담당한다. 역할을 섞지 마라.
 */
@Mapper
public interface AuthQueryMapper {

    /**
     * 로그인 응답 · 내 정보 조회에 필요한 값 전부.
     *
     * <p>{@code department} 를 두 번 조인한다 — 자기 부서(d)와 상위 부서(p).
     * 명세의 {@code departmentPath}(`기술본부 / 개발팀`)가 2단 구조를 요구한다.
     */
    @Select("""
            SELECT e.user_id              AS userId,
                   e.name                 AS name,
                   a.role                 AS role,
                   a.must_change_password AS mustChangePassword,
                   e.email                AS email,
                   e.phone                AS phone,
                   d.name                 AS departmentName,
                   p.name                 AS parentDepartmentName,
                   jp.name                AS jobPositionName,
                   e.hired_at             AS hiredAt,
                   a.last_login_at        AS lastLoginAt
              FROM account a
              JOIN employee e     ON e.user_id = a.user_id
              LEFT JOIN department d  ON d.department_id = e.department_id
              LEFT JOIN department p  ON p.department_id = d.parent_id
              LEFT JOIN job_position jp ON jp.job_position_id = e.job_position_id
             WHERE a.user_id = #{userId}
            """)
    Optional<UserProfileRow> findProfile(@Param("userId") String userId);
}
