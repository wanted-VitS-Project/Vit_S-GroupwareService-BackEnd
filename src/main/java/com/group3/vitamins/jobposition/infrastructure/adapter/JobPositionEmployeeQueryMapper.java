package com.group3.vitamins.jobposition.infrastructure.adapter;

import com.group3.vitamins.jobposition.application.result.JobPositionEmployeeRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 직급별 사원 목록 조회 (사원 도메인 소관 {@code employee} 테이블).
 * 남의 테이블에 JPA 엔티티를 만들면 소유가 흐려지므로 MyBatis 로 읽는다 ({@code JobPositionEmployeeCountMapper} 선례).
 *
 * <p>⛔ SQL 은 애노테이션이 아니라 XML 에 둔다 (팀 MyBatis 컨벤션) —
 * {@code src/main/resources/mapper/jobposition/JobPositionEmployeeQueryMapper.xml}. 여기엔 메서드 선언만 있다.
 * XML 의 {@code namespace} 는 이 인터페이스 전체 경로, {@code id} 는 메서드명과 같다.
 */
@Mapper
public interface JobPositionEmployeeQueryMapper {

    /**
     * 직급에 속한 사원을 이름 오름차순(동명이인은 사번 오름차순)으로 조회한다.
     * 시스템 계정·퇴사자·삭제 사원 제외 (`.ai/api/job-position.md` §5).
     */
    List<JobPositionEmployeeRow> findEmployeesByJobPosition(@Param("jobPositionId") Long jobPositionId);
}
