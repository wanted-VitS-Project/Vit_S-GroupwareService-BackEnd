package com.group3.vitamins.department.infrastructure.persistence;

import com.group3.vitamins.department.domain.model.Department;
import com.group3.vitamins.department.domain.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * {@link DepartmentRepository} 포트의 JPA 어댑터.
 *
 * <p>{@link #save} 는 {@code saveAndFlush} 로 즉시 반영해, 유니크 제약 위반을 커밋까지 미루지 않고
 * 이 시점에 드러낸다 — 서비스가 그 위반을 명세의 409({@code DEPT_NAME_DUPLICATED})로 변환할 수 있게 한다.
 */
@Repository
@RequiredArgsConstructor
public class DepartmentRepositoryAdapter implements DepartmentRepository {

    private final SpringDataDepartmentRepository springDataRepository;

    @Override
    public Department save(Department department) {
        DepartmentJpaEntity saved =
                springDataRepository.saveAndFlush(DepartmentPersistenceMapper.toEntity(department));
        return DepartmentPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<Department> findById(Long departmentId) {
        return springDataRepository.findById(departmentId)
                .map(DepartmentPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Department> findByIdForUpdate(Long departmentId) {
        return springDataRepository.findByIdForUpdate(departmentId)
                .map(DepartmentPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsSiblingName(String name, Long parentId) {
        // MySQL/H2 는 parent_id 가 NULL 인 행끼리 UNIQUE 로 안 막으므로, 최상위(부모 없음)는
        // IS NULL 파생 쿼리로 따로 센다. (파생 쿼리에 null 을 그대로 넘기면 `= null` 이 되어 항상 false)
        return parentId == null
                ? springDataRepository.existsByNameAndParentIdIsNull(name)
                : springDataRepository.existsByNameAndParentId(name, parentId);
    }

    @Override
    public boolean existsSiblingNameExcludingSelf(String name, Long parentId, Long departmentId) {
        return parentId == null
                ? springDataRepository.existsByNameAndParentIdIsNullAndDepartmentIdNot(name, departmentId)
                : springDataRepository.existsByNameAndParentIdAndDepartmentIdNot(name, parentId, departmentId);
    }

    @Override
    public long countByParentId(Long parentId) {
        return springDataRepository.countByParentId(parentId);
    }

    @Override
    public void delete(Department department) {
        springDataRepository.deleteById(department.getDepartmentId());
    }
}
