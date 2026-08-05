package com.group3.vitamins.auth.infrastructure.adapter;

import com.group3.vitamins.auth.application.result.UserProfileRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * 인증 화면용 조회. <b>MyBatis 를 쓰는 이유는 애그리거트 4개를 가로지르기 때문</b>이다
 * (계정 · 사원 · 부서 · 직급). JPA 로 짜면 연관관계를 타느라 쿼리가 여러 번 나간다.
 *
 * <p>쓰기는 JPA({@code AccountEntity})가 담당한다. 역할을 섞지 마라.
 * SQL 은 XML 에 둔다 — {@code src/main/resources/mapper/auth/AuthQueryMapper.xml} (namespace = 이 인터페이스 FQN).
 */
@Mapper
public interface AuthQueryMapper {

    /**
     * 로그인 응답 · 내 정보 조회에 필요한 값 전부.
     *
     * <p>{@code department} 를 두 번 조인한다 — 자기 부서(d)와 상위 부서(p).
     * 명세의 {@code departmentPath}(`기술본부 / 개발팀`)가 2단 구조를 요구한다.
     */
    Optional<UserProfileRow> findProfile(@Param("userId") String userId);
}
