package com.group3.vitamins.text.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataTextRepository extends JpaRepository<TextJpaEntity, Long> {
}
