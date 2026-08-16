package com.group3.vitamins.project.step.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.application.usecase.ProjectAccessUseCase;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.step.application.policy.StepAccessPolicy;
import com.group3.vitamins.project.step.application.port.IssueStatLookupPort;
import com.group3.vitamins.project.step.application.query.StepDetailQuery;
import com.group3.vitamins.project.step.application.query.StepListQuery;
import com.group3.vitamins.project.step.application.result.StepDetailResult;
import com.group3.vitamins.project.step.application.result.StepPerson;
import com.group3.vitamins.project.step.application.result.StepSummary;
import com.group3.vitamins.project.step.application.usecase.StepAccessUseCase;
import com.group3.vitamins.project.step.application.usecase.StepQueryUseCase;
import com.group3.vitamins.project.step.domain.exception.StepErrorCode;
import com.group3.vitamins.project.step.domain.model.Step;
import com.group3.vitamins.project.step.domain.repository.StepPermissionRepository;
import com.group3.vitamins.project.step.domain.repository.StepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StepQueryService implements StepQueryUseCase {

    private final StepRepository stepRepository;
    private final StepPermissionRepository stepPermissionRepository;
    private final IssueStatLookupPort issueStatLookupPort;
    private final EmployeeLookupPort employeeLookupPort;
    private final ProjectAccessUseCase projectAccessUseCase;
    private final StepAccessUseCase stepAccessUseCase;
    private final StepAccessPolicy stepAccessPolicy;

    @Override
    public List<StepSummary> getSteps(StepListQuery query) {
        MemberPermission projectPermission = projectAccessUseCase.requireAccess(
                query.projectId(), query.requesterUserId(), query.role());

        List<Step> steps = stepRepository.search(
                query.projectId(), query.stageId(), query.status());
        if (steps.isEmpty()) {
            return List.of();
        }

        Map<Long, MemberPermission> permissions =
                resolvePermissions(steps, projectPermission, query.requesterUserId(), query.role());
        List<Step> visible = steps.stream()
                .filter(step -> permissions.containsKey(step.getStepId()))
                .toList();
        if (visible.isEmpty()) {
            return List.of();
        }

        Map<Long, IssueStatLookupPort.IssueStatView> issueStats = issueStatLookupPort
                .countByStepIds(visible.stream().map(Step::getStepId).toList());
        Map<String, EmployeeLookupPort.EmployeeRef> names = findNames(visible.stream()
                .map(Step::getOwnerUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        return visible.stream()
                .map(step -> toSummary(step, permissions, issueStats, names))
                .toList();
    }

    /**
     * 판정은 {@link StepAccessUseCase} 에 맡긴다 — 예전에는 같은 규칙(프로젝트 권한 → 스텝 오버라이드)을
     * 여기에 복제해 뒀는데, 두 벌이 되면 한쪽만 고쳤을 때 조용히 갈라진다. 404 → 403 순서도 그쪽이 갖는다.
     */
    @Override
    public StepDetailResult getStepDetail(StepDetailQuery query) {
        MemberPermission myPermission = stepAccessUseCase
                .requireAccess(query.stepId(), query.requesterUserId(), query.role())
                .permission();

        Step step = stepRepository.findById(query.stepId())
                .orElseThrow(() -> new NotFoundException(StepErrorCode.STEP_NOT_FOUND));

        IssueStatLookupPort.IssueStatView stat = issueStatLookupPort
                .countByStepIds(List.of(step.getStepId()))
                .getOrDefault(step.getStepId(), IssueStatLookupPort.IssueStatView.empty());

        Map<String, EmployeeLookupPort.EmployeeRef> names = findNames(
                Stream.of(step.getOwnerUserId(), step.getCompletedBy())
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()));

        return new StepDetailResult(
                step.getStepId(), step.getProjectId(), step.getStageId(), step.getName(),
                step.getStatus().name(), step.getStartedOn(), step.getEndedOn(),
                toPerson(step.getOwnerUserId(), names),
                stat.totalCount(), stat.doneCount(), stat.inProgressCount(),
                progressRate(stat),
                toPerson(step.getCompletedBy(), names),
                step.getCompletedAt(), myPermission.name(), step.getVersion());
    }

    /** 스텝별 유효 권한. NONE 인 스텝은 키를 넣지 않아 목록에서 빠진다 (STP-010). */
    private Map<Long, MemberPermission> resolvePermissions(List<Step> steps,
                                                           MemberPermission projectPermission,
                                                           String requesterUserId, String role) {
        Map<Long, MemberPermission> overrides = stepPermissionRepository.findOverrides(
                steps.stream().map(Step::getStepId).toList(), requesterUserId);

        Map<Long, MemberPermission> permissions = new HashMap<>();
        for (Step step : steps) {
            MemberPermission permission = stepAccessPolicy.resolve(
                    role, projectPermission, overrides.get(step.getStepId()));
            if (stepAccessPolicy.isAccessible(permission)) {
                permissions.put(step.getStepId(), permission);
            }
        }
        return permissions;
    }

    private StepSummary toSummary(Step step, Map<Long, MemberPermission> permissions,
                                  Map<Long, IssueStatLookupPort.IssueStatView> issueStats,
                                  Map<String, EmployeeLookupPort.EmployeeRef> names) {
        IssueStatLookupPort.IssueStatView stat = issueStats.getOrDefault(
                step.getStepId(), IssueStatLookupPort.IssueStatView.empty());

        return new StepSummary(
                step.getStepId(), step.getStageId(), step.getName(), step.getStatus().name(),
                step.getSortOrder(), step.getStartedOn(), step.getEndedOn(),
                toPerson(step.getOwnerUserId(), names),
                stat.totalCount(), stat.doneCount(), stat.inProgressCount(),
                progressRate(stat),
                permissions.get(step.getStepId()).name(),
                step.getVersion());
    }

    /** 완료 이슈 / 전체 이슈 비율. 이슈가 없으면 null 을 돌려 응답에서 빠지게 한다 (INV-04). */
    private Integer progressRate(IssueStatLookupPort.IssueStatView stat) {
        if (stat.totalCount() == 0) {
            return null;
        }
        return stat.doneCount() * 100 / stat.totalCount();
    }

    /**
     * 사번을 안 보냈으면 null, 사번을 못 찾으면 사번만 담는다 (응답이 깨지지 않게).
     *
     * <p>삭제된 사원은 이름을 그대로 담고 {@code deleted = true} 로 알린다 (DELETE.md D-6).
     * 이름이 {@code null} 인 건 <b>사번이 employee 에 아예 없다</b>는 뜻이다.
     */
    private StepPerson toPerson(String userId, Map<String, EmployeeLookupPort.EmployeeRef> names) {
        if (userId == null) {
            return null;
        }
        EmployeeLookupPort.EmployeeRef ref = names.get(userId);
        return ref == null
                ? new StepPerson(userId, null, false)
                : new StepPerson(userId, ref.name(), ref.deleted());
    }

    private Map<String, EmployeeLookupPort.EmployeeRef> findNames(Set<String> userIds) {
        return employeeLookupPort.findRefsByUserIds(userIds);
    }
}