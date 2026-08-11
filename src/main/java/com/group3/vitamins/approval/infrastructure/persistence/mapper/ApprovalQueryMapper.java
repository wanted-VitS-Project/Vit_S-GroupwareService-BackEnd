package com.group3.vitamins.approval.infrastructure.persistence.mapper;

import com.group3.vitamins.approval.infrastructure.persistence.row.ApprovalLineDetailRow;
import com.group3.vitamins.approval.infrastructure.persistence.row.ApprovalEmployeeRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 회차 상세조회(MGT-005) 전용 조회. {@code approval_line}이 {@code employee}·{@code department}·
 * {@code job_position}을 가로지르는 조인이라 MyBatis를 쓴다({@code MYBATIS.md} §1).
 *
 * <p>쓰기는 JPA(`ApprovalLineJpaEntity`)가 담당한다 — 역할을 섞지 않는다.
 */
@Mapper
public interface ApprovalQueryMapper {

    Optional<ApprovalEmployeeRow> findEmployee(@Param("userId") String userId);

    List<ApprovalLineDetailRow> findLineDetailsByRevisionId(@Param("revisionId") Long revisionId);
}
