package com.group3.vitamins.project.infrastructure.adapter;

import com.group3.vitamins.project.application.port.ProjectListQueryPort;
import com.group3.vitamins.project.application.query.ProjectListCriteria;
import com.group3.vitamins.project.application.result.BusinessCategorySummary;
import com.group3.vitamins.project.application.result.MemberBrief;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProjectListQueryAdapter implements ProjectListQueryPort {

    private final ProjectListQueryMapper projectListQueryMapper;

    @Override
    public List<ProjectListView> findPage(ProjectListCriteria criteria) {
        return projectListQueryMapper.findPage(criteria).stream()
                .collect(Collectors.groupingBy(
                        ProjectListRow::projectId, LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .map(this::toView)
                .toList();
    }

    @Override
    public long count(ProjectListCriteria criteria) {
        return projectListQueryMapper.count(criteria);
    }

    /** 같은 프로젝트의 행들을 하나로 접는다. 스칼라 값은 어느 행이든 같다. */
    private ProjectListView toView(List<ProjectListRow> rows) {
        ProjectListRow head = rows.get(0);
        return new ProjectListView(
                head.projectId(), head.name(), head.clientName(), head.status(),
                head.startedOn(), head.endedOn(), head.contractAmount(),
                head.totalStepCount(), head.doneStepCount(),
                head.myIssueInProgressCount(), head.myApprovalInProgressCount(),
                foldCategories(rows), foldMembers(rows), head.version());
    }

    /** 참여자 수만큼 중복된 카테고리를 id 기준으로 접는다. */
    private List<BusinessCategorySummary> foldCategories(List<ProjectListRow> rows) {
        return rows.stream()
                .filter(row -> row.categoryId() != null)
                .collect(Collectors.toMap(
                        ProjectListRow::categoryId,
                        row -> new BusinessCategorySummary(
                                row.categoryId(), row.categoryName(), row.categoryCode(), row.categoryDeleted()),
                        (first, duplicate) -> first,
                        LinkedHashMap::new))
                .values().stream().toList();
    }

    /**
     * 카테고리 수만큼 중복된 참여자를 사번 기준으로 접는다.
     *
     * <p>⚠️ 걸러내는 기준은 <b>사번</b>이지 이름이 아니다. 이름으로 거르면 삭제된 사원의 참여자가
     * 목록에서 통째로 사라져 SQL 에서 살려낸 행(DELETE.md 패턴 A → D)을 여기서 다시 죽인다.
     * 사번까지 null 인 행은 참여자가 없는 프로젝트의 LEFT JOIN 빈 행이다 — 접기 키가 없어 반드시 뺀다.
     */
    private List<MemberBrief> foldMembers(List<ProjectListRow> rows) {
        return rows.stream()
                .filter(row -> row.memberUserId() != null)
                .collect(Collectors.toMap(
                        ProjectListRow::memberUserId,
                        row -> new MemberBrief(row.memberUserId(), row.memberName(), row.memberDeleted()),
                        (first, duplicate) -> first,
                        LinkedHashMap::new))
                .values().stream().toList();
    }
}