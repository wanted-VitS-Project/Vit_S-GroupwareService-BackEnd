package com.group3.vitamins.project.application.port;

import com.group3.vitamins.project.application.query.ProjectListCriteria;
import com.group3.vitamins.project.application.result.BusinessCategorySummary;
import com.group3.vitamins.project.application.result.MemberBrief;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 필터·페이징이 적용된 프로젝트 목록을 조회하는 아웃바운드 포트. */
public interface ProjectListQueryPort {

    /** 조건에 맞는 한 페이지를 카테고리·참여자·집계까지 채워 조회한다. */
    List<ProjectListView> findPage(ProjectListCriteria criteria);

    /** 조건에 맞는 전체 건수를 센다. */
    long count(ProjectListCriteria criteria);

    record ProjectListView(
            Long projectId,
            String name,
            String clientName,
            String status,
            LocalDate startedOn,
            LocalDate endedOn,
            BigDecimal contractAmount,
            int totalStepCount,
            int doneStepCount,
            int myIssueInProgressCount,
            int myApprovalInProgressCount,
            List<BusinessCategorySummary> businessCategories,
            List<MemberBrief> members,
            int version
    ) {}
}