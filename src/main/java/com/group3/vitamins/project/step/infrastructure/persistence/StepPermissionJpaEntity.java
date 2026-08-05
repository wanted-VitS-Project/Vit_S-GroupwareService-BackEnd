package com.group3.vitamins.project.step.infrastructure.persistence;

import com.group3.vitamins.project.domain.model.MemberPermission;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "step_permission")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StepPermissionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "step_permission_id")
    private Long stepPermissionId;

    @Column(name = "step_id", nullable = false)
    private Long stepId;

    @Column(name = "user_id", nullable = false, length = 20)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false)
    private MemberPermission permission;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
