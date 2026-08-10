package com.group3.vitamins.project.block.infrastructure.adapter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 이슈 애그리게이트 소관 연결 테이블({@code issue_block}) 직접 정리 — 블록 이동용.
 *
 * <p>이슈 도메인에는 <b>블록 기준</b> 해제 경로가 없다({@code deleteBlockLinks} 는 이슈 기준).
 * 담긴 정보가 없는 순수 연결 행이고 이슈 규칙이 개입할 여지가 없어 여기서 직접 지운다.
 * 블록 조회가 이미 같은 테이블을 직접 읽는 선례가 있다({@code BlockIssueStatQueryMapper}).
 *
 * <p>⛔ SQL 은 애노테이션이 아니라 XML 에 둔다 (팀 MyBatis 컨벤션) —
 * {@code src/main/resources/mapper/block/IssueBlockUnlinkMapper.xml}.
 */
@Mapper
public interface IssueBlockUnlinkMapper {

    int deleteByBlockId(@Param("blockId") Long blockId);
}
