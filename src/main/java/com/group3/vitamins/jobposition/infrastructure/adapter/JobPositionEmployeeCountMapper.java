package com.group3.vitamins.jobposition.infrastructure.adapter;

import com.group3.vitamins.jobposition.application.result.JobPositionEmployeeCountRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 직급별 사용 인원 집계 (사원 도메인 소관 {@code employee} 테이블).
 * 남의 테이블에 JPA 엔티티를 만들면 소유가 흐려지므로 MyBatis 로 읽는다 ({@code DepartmentEmployeeQueryMapper} 선례).
 *
 * <p>⛔ SQL 은 애노테이션이 아니라 XML 에 둔다 (팀 MyBatis 컨벤션) —
 * {@code src/main/resources/mapper/jobposition/JobPositionEmployeeCountMapper.xml}. 여기엔 메서드 선언만 있다.
 * XML 의 {@code namespace} 는 이 인터페이스 전체 경로, {@code id} 는 메서드명과 같다.
 */
@Mapper
public interface JobPositionEmployeeCountMapper {

    /**
     * 직급별 사용 인원 (인원 0 인 직급은 결과에 없다).
     * 시스템 계정·퇴사자·삭제 사원 제외 (`.ai/api/job-position.md` POS-002).
     */
    List<JobPositionEmployeeCountRow> countByJobPosition();

    /** 직급 1건의 표시용 사용 인원 — 수정 응답용. 목록과 같은 제외 기준. */
    long countByJobPositionId(@Param("jobPositionId") Long jobPositionId);

    /** 직급을 참조하는 모든 사원 수(필터 없음) — 삭제 차단 판정용(FK 위반 방지). */
    long countAllReferencing(@Param("jobPositionId") Long jobPositionId);
}
