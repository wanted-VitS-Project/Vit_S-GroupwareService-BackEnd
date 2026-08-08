package com.group3.vitamins.employeegroup.infrastructure.persistence;

import com.group3.vitamins.employeegroup.domain.repository.EmployeeGroupMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class EmployeeGroupMemberRepositoryAdapter implements EmployeeGroupMemberRepository {

    private final SpringDataEmployeeGroupMemberRepository springDataRepository;

    @Override
    public Set<String> findMemberUserIds(Long groupId) {
        return springDataRepository.findByGroupId(groupId).stream()
                .map(EmployeeGroupMemberJpaEntity::getUserId)
                .collect(Collectors.toSet());
    }

    @Override
    public void addMembers(Long groupId, Collection<String> userIds) {
        if (userIds.isEmpty()) {
            return;
        }
        springDataRepository.saveAll(userIds.stream()
                .map(userId -> new EmployeeGroupMemberJpaEntity(groupId, userId))
                .toList());
    }

    @Override
    public boolean existsMember(Long groupId, String userId) {
        return springDataRepository.existsByGroupIdAndUserId(groupId, userId);
    }

    @Override
    public void removeMember(Long groupId, String userId) {
        springDataRepository.deleteByGroupIdAndUserId(groupId, userId);
    }
}
