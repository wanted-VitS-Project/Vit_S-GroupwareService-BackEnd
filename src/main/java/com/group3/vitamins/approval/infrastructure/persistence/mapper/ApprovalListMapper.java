package com.group3.vitamins.approval.infrastructure.persistence.mapper;

import com.group3.vitamins.approval.infrastructure.persistence.row.ApprovalLinePreviewRow;
import com.group3.vitamins.approval.infrastructure.persistence.row.ApprovalListRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * 결재관리 목록조회(MGT-001~004) 전용 조회. {@code approval}이 현재 회차·{@code block}/{@code step}/
 * {@code project}·기안자/현재 결재자({@code employee})를 가로지르는 조인이라 MyBatis를 쓴다(`MYBATIS.md` §1).
 *
 * <p>{@code scope}(drafted/pending/all) 해석은 서비스가 끝내고, 이 매퍼는 이미 확정된
 * {@code drafterId}/{@code approverId}(전체 조회)/{@code activeApproverId}(pending 전용)만 받는다.
 */
@Mapper
public interface ApprovalListMapper {

    long countApprovals(@Param("status") String status,
                         @Param("drafterId") String drafterId,
                         @Param("approverId") String approverId,
                         @Param("activeApproverId") String activeApproverId,
                         @Param("fromDate") LocalDate fromDate,
                         @Param("toDate") LocalDate toDate,
                         @Param("keyword") String keyword,
                         @Param("revisionNo") Integer revisionNo);

    List<ApprovalListRow> findApprovals(@Param("status") String status,
                                         @Param("drafterId") String drafterId,
                                         @Param("approverId") String approverId,
                                         @Param("activeApproverId") String activeApproverId,
                                         @Param("fromDate") LocalDate fromDate,
                                         @Param("toDate") LocalDate toDate,
                                         @Param("keyword") String keyword,
                                         @Param("revisionNo") Integer revisionNo,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    /** 반환된 현재 회차들의 결재선 전체(아바타 미리보기용) 배치 조회. */
    List<ApprovalLinePreviewRow> findLinePreviewsByRevisionIds(@Param("revisionIds") Collection<Long> revisionIds);
}
