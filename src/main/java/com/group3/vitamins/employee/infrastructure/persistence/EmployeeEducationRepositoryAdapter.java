package com.group3.vitamins.employee.infrastructure.persistence;

import com.group3.vitamins.employee.domain.model.EmployeeEducation;
import com.group3.vitamins.employee.domain.repository.EmployeeEducationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class EmployeeEducationRepositoryAdapter implements EmployeeEducationRepository {

    private final SpringDataEmployeeEducationRepository springDataRepository;

    @Override
    public void saveAll(List<EmployeeEducation> educations) {
        if (educations.isEmpty()) {
            return;
        }
        springDataRepository.saveAll(educations.stream()
                .map(e -> new EmployeeEducationJpaEntity(
                        e.companyId(), e.userId(), e.majorId(), e.degree(), e.school()))
                .toList());
    }

    @Override
    public void deleteByUserId(String userId) {
        springDataRepository.deleteByUserId(userId);
    }
}
