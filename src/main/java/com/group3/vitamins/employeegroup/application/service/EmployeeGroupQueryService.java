package com.group3.vitamins.employeegroup.application.service;

import com.group3.vitamins.employeegroup.application.port.EmployeeGroupQueryPort;
import com.group3.vitamins.employeegroup.application.result.GroupListRow;
import com.group3.vitamins.employeegroup.application.result.GroupMembersResult;
import com.group3.vitamins.employeegroup.application.result.MemberRow;
import com.group3.vitamins.employeegroup.application.usecase.EmployeeGroupQueryUseCase;
import com.group3.vitamins.employeegroup.domain.exception.EmployeeGroupErrorCode;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 그룹 조회 (§1 목록 · §3 수정 응답 재조회). 목록·구성원 집계는 MyBatis({@link EmployeeGroupQueryPort}) 담당.
 * 목록은 전체 사용자가 호출하므로 ADMIN 판정을 걸지 않는다(선택 도구).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeGroupQueryService implements EmployeeGroupQueryUseCase {

    private final EmployeeGroupQueryPort queryPort;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;

    @Override
    public List<GroupListRow> listGroups(String keyword) {
        String normalized = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return queryPort.findGroups(normalized, currentCompanyIdProvider.currentCompanyId());
    }

    @Override
    public GroupListRow getGroup(Long groupId) {
        return queryPort.findGroup(groupId, currentCompanyIdProvider.currentCompanyId())
                .orElseThrow(() -> new NotFoundException(EmployeeGroupErrorCode.GRP_NOT_FOUND));
    }

    @Override
    public GroupMembersResult getMembers(Long groupId) {
        // 그룹 존재 확인 + 그룹명 확보(응답에 name 필요). 회사 범위 조회라 타사 그룹은 404.
        GroupListRow group = queryPort.findGroup(groupId, currentCompanyIdProvider.currentCompanyId())
                .orElseThrow(() -> new NotFoundException(EmployeeGroupErrorCode.GRP_NOT_FOUND));

        List<GroupMembersResult.Member> members = queryPort.findMembers(groupId).stream()
                .map(r -> new GroupMembersResult.Member(
                        r.userId(), r.name(), departmentPath(r), r.jobPositionName(), r.addedAt()))
                .toList();

        return new GroupMembersResult(group.groupId(), group.name(), members);
    }

    /** 상위부서명 + 부서명으로 "기술본부 / 개발팀" 조립 (jobposition 선례). 부서 없으면 null. */
    private String departmentPath(MemberRow r) {
        if (r.departmentName() == null) {
            return null;
        }
        return r.parentDepartmentName() == null
                ? r.departmentName()
                : r.parentDepartmentName() + " / " + r.departmentName();
    }
}
