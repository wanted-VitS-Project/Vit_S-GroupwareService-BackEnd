package com.group3.vitamins.approval.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataApprovalRepository extends JpaRepository<ApprovalJpaEntity, Long> {
}
