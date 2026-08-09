package com.group3.vitamins.employeegroup.infrastructure.persistence;

import com.group3.vitamins.employeegroup.domain.repository.EmployeeGroupMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class EmployeeGroupMemberRepositoryAdapter implements EmployeeGroupMemberRepository {

    private final SpringDataEmployeeGroupMemberRepository springDataRepository;

    @Override
    public Set<String> findExistingMemberUserIds(Long groupId, Collection<String> userIds) {
        if (userIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(springDataRepository.findExistingUserIds(groupId, userIds));
    }

    @Override
    public void addMembers(Long groupId, Collection<String> userIds) {
        if (userIds.isEmpty()) {
            return;
        }
        // saveAllAndFlush — 뒤이어 같은 트랜잭션에서 MyBatis 로 구성원 수를 읽으므로 INSERT 를 즉시 반영시킨다.
        springDataRepository.saveAllAndFlush(userIds.stream()
                .map(userId -> new EmployeeGroupMemberJpaEntity(groupId, userId))
                .toList());
    }

    @Override
    public boolean existsMember(Long groupId, String userId) {
        return springDataRepository.existsByGroupIdAndUserId(groupId, userId);
    }

    @Override
    public void removeMember(Long groupId, String userId) {
        springDataRepository.deleteMember(groupId, userId); // 벌크 DELETE — 즉시 반영
    }
}
