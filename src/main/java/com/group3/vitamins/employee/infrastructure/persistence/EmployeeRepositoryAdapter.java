package com.group3.vitamins.employee.infrastructure.persistence;

import com.group3.vitamins.employee.domain.model.Employee;
import com.group3.vitamins.employee.domain.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * {@link EmployeeRepository} 포트의 JPA 어댑터.
 *
 * <p>{@link #save} 는 {@code saveAndFlush} 로 즉시 반영해, PK({@code user_id}) 중복을 커밋까지 미루지 않고
 * 이 시점에 드러낸다 — 등록 유스케이스가 그 위반을 명세의 409({@code EMP_USER_ID_DUPLICATED})로 변환할 수
 * 있게 한다 (department 어댑터의 {@code saveAndFlush} 선례).
 */
@Repository
@RequiredArgsConstructor
public class EmployeeRepositoryAdapter implements EmployeeRepository {

    private final SpringDataEmployeeRepository springDataRepository;

    @Override
    public Employee save(Employee employee) {
        EmployeeJpaEntity saved =
                springDataRepository.saveAndFlush(EmployeePersistenceMapper.toEntity(employee));
        return EmployeePersistenceMapper.toDomain(saved);
    }

    @Override
    public boolean existsById(String userId) {
        return springDataRepository.existsById(userId);
    }
}
