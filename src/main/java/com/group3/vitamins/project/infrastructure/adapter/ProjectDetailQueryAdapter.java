package com.group3.vitamins.project.infrastructure.adapter;

import com.group3.vitamins.project.application.port.ProjectDetailQueryPort;
import com.group3.vitamins.project.application.result.BusinessCategorySummary;
import com.group3.vitamins.project.domain.model.MemberPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProjectDetailQueryAdapter implements ProjectDetailQueryPort {

    private final ProjectDetailQueryMapper projectDetailQueryMapper;

    @Override
    public Optional<ProjectDetailView> findDetail(Long projectId, String requesterUserId) {
        List<ProjectDetailRow> rows = projectDetailQueryMapper.findDetail(projectId, requesterUserId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toView(rows));
    }

    /** 카테고리 수만큼 반복된 행을 하나로 접는다. 스칼라 값은 어느 행이든 같다. */
    private ProjectDetailView toView(List<ProjectDetailRow> rows) {
        ProjectDetailRow head = rows.get(0);
        return new ProjectDetailView(
                head.projectId(), head.name(), head.description(), head.clientName(), head.status(),
                head.startedOn(), head.endedOn(), head.contractAmount(),
                head.stepCount(), head.doneStepCount(),
                head.bidNoticeId(), head.closeReasonCode(), head.closeReasonNote(),
                toPermission(head.memberPermission()), head.createdAt(),
                categoriesOf(rows));
    }

    /** 연결이 없으면 LEFT JOIN 이 만든 빈 행 하나만 오므로 걸러낸다. */
    private List<BusinessCategorySummary> categoriesOf(List<ProjectDetailRow> rows) {
        return rows.stream()
                .filter(row -> row.categoryId() != null)
                .map(row -> new BusinessCategorySummary(
                        row.categoryId(), row.categoryName(), row.categoryCode()))
                .toList();
    }

    /** 참여자 행이 없으면 null 그대로 넘긴다 — 403 판정은 정책이 한다. */
    private MemberPermission toPermission(String permission) {
        return permission == null ? null : MemberPermission.valueOf(permission);
    }
}