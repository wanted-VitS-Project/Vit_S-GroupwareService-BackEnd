package com.group3.vitamins.approval.infrastructure.blockdetail;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/** 블록 조회용 결재 미리보기 배치 조회(BND-003). 쓰기는 JPA(ApprovalRepository)가 담당한다. */
@Mapper
public interface ApprovalDetailMapper {

    /** approval 하나당 최신 회차(revision_no 최대) 1행. */
    List<ApprovalRevisionRow> findLatestRevisions(@Param("approvalIds") Collection<Long> approvalIds);

    /** 해당 회차들의 결재선 전체(진행 현황 카운트는 호출자가 센다). */
    List<ApprovalLineRow> findLinesByRevisionIds(@Param("revisionIds") Collection<Long> revisionIds);
}
