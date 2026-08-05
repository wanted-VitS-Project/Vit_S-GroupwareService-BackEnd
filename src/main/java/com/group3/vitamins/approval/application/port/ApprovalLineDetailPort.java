package com.group3.vitamins.approval.application.port;

import com.group3.vitamins.approval.application.result.ApprovalLineDetailView;

import java.util.List;

/**
 * 회차 상세조회(MGT-005)의 결재선+직원 조인 조회 포트. 구현체는 MyBatis({@code ApprovalQueryMapper})를 쓴다
 * — 결재선마다 {@link EmployeeCatalogPort#findEmployee}를 따로 부르던 N+1을 조인 한 번으로 대체하기 위함
 * ({@code MYBATIS.md} §1: 여러 테이블을 조인해서 조회해야 할 때).
 */
public interface ApprovalLineDetailPort {

    List<ApprovalLineDetailView> findLineDetails(Long revisionId);
}
