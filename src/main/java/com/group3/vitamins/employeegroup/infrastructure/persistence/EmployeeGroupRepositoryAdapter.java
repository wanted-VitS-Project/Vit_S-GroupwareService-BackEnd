package com.group3.vitamins.employeegroup.infrastructure.persistence;

import com.group3.vitamins.employeegroup.domain.model.EmployeeGroup;
import com.group3.vitamins.employeegroup.domain.repository.EmployeeGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * {@link EmployeeGroupRepository} 포트의 JPA 어댑터. {@link #save} 는 {@code saveAndFlush} 로 유니크 위반을
 * 즉시 드러내 서비스가 {@code GRP_NAME_DUPLICATED}(409)로 변환할 수 있게 한다.
 */
@Repository
@RequiredArgsConstructor
public class EmployeeGroupRepositoryAdapter implements EmployeeGroupRepository {

    private final SpringDataEmployeeGroupRepository springDataRepository;

    @Override
    public EmployeeGroup save(EmployeeGroup group) {
        EmployeeGroupJpaEntity saved =
                springDataRepository.saveAndFlush(EmployeeGroupPersistenceMapper.toEntity(group));
        return EmployeeGroupPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<EmployeeGroup> findById(Long groupId) {
        return springDataRepository.findById(groupId).map(EmployeeGroupPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByName(String name) {
        return springDataRepository.existsByName(name);
    }

    @Override
    public boolean existsByNameExcludingSelf(String name, Long groupId) {
        return springDataRepository.existsByNameAndGroupIdNot(name, groupId);
    }

    @Override
    public void delete(EmployeeGroup group) {
        springDataRepository.deleteById(group.getGroupId());
    }
}
