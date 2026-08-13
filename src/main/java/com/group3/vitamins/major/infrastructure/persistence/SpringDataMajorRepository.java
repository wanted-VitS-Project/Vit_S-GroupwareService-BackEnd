package com.group3.vitamins.major.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataMajorRepository extends JpaRepository<MajorJpaEntity, Long> {

    Optional<MajorJpaEntity> findByMajorIdAndCompanyId(Long majorId, Long companyId);

    Optional<MajorJpaEntity> findByCompanyIdAndName(Long companyId, String name);
}
