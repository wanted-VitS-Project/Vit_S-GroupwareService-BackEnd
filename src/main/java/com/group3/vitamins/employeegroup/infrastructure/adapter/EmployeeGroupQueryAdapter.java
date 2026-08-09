package com.group3.vitamins.employeegroup.infrastructure.adapter;

import com.group3.vitamins.employeegroup.application.port.EmployeeGroupQueryPort;
import com.group3.vitamins.employeegroup.application.result.EmployeeRefRow;
import com.group3.vitamins.employeegroup.application.result.GroupListRow;
import com.group3.vitamins.employeegroup.application.result.MemberRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EmployeeGroupQueryAdapter implements EmployeeGroupQueryPort {

    private final EmployeeGroupQueryMapper mapper;

    @Override
    public List<GroupListRow> findGroups(String keyword) {
        return mapper.findGroups(keyword);
    }

    @Override
    public Optional<GroupListRow> findGroup(Long groupId) {
        return Optional.ofNullable(mapper.findGroup(groupId));
    }

    @Override
    public List<MemberRow> findMembers(Long groupId) {
        return mapper.findMembers(groupId);
    }

    @Override
    public int countMembers(Long groupId) {
        return mapper.countMembers(groupId);
    }

    @Override
    public List<EmployeeRefRow> findEmployeeRefs(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return mapper.findEmployeeRefs(userIds);
    }
}
