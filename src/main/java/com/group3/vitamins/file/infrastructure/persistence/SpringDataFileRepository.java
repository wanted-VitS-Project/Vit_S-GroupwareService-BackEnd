package com.group3.vitamins.file.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataFileRepository extends JpaRepository<FileJpaEntity, Long> {
}
