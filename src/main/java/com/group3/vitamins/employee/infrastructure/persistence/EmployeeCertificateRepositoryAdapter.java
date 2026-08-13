package com.group3.vitamins.employee.infrastructure.persistence;

import com.group3.vitamins.employee.domain.model.EmployeeCertificate;
import com.group3.vitamins.employee.domain.repository.EmployeeCertificateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class EmployeeCertificateRepositoryAdapter implements EmployeeCertificateRepository {

    private final SpringDataEmployeeCertificateRepository springDataRepository;

    @Override
    public void saveAll(List<EmployeeCertificate> certificates) {
        if (certificates.isEmpty()) {
            return;
        }
        springDataRepository.saveAll(certificates.stream()
                .map(c -> new EmployeeCertificateJpaEntity(
                        c.companyId(), c.userId(), c.certificateId(), c.acquiredDate()))
                .toList());
    }

    @Override
    public void deleteByUserId(String userId) {
        springDataRepository.deleteByUserId(userId);
    }
}
