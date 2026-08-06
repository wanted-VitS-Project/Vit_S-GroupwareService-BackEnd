package com.group3.vitamins.file.infrastructure.adapter;

import com.group3.vitamins.file.application.port.ApprovalLockQueryPort;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 결재 잠금 조회 (MyBatis · 조회 전용). SQL 은 XML 에 둔다. */
@Mapper
public interface ApprovalLockMapper {

    /** 문서의 버전 중 진행 중 결재의 대상이 있으면 그 결재 정보를 돌려준다. 없으면 null. */
    ApprovalLockQueryPort.InProgressApproval findInProgressApproval(@Param("fileId") Long fileId);
}
