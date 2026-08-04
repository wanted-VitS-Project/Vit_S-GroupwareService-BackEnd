package com.group3.vitamins.activitylog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogJpaRepository extends JpaRepository<ActivityLogEntity, Long> {
}
