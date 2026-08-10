package com.group3.vitamins.project.infrastructure.persistence;

import com.group3.vitamins.project.domain.model.CloseReasonCode;
import com.group3.vitamins.project.domain.model.ProjectStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "project",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_project_bid_notice_company", columnNames = {"bid_notice_id", "company_id"}))
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "bid_notice_id")
    private Long bidNoticeId;

    @Column(name = "name", nullable = false, length = 300)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProjectStatus status;

    @Column(name = "client_name", length = 200)
    private String clientName;

    @Column(name = "contract_amount", precision = 18, scale = 2)
    private BigDecimal contractAmount;

    @Column(name = "started_on")
    private LocalDate startedOn;

    @Column(name = "ended_on")
    private LocalDate endedOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "close_reason_code")
    private CloseReasonCode closeReasonCode;

    @Column(name = "close_reason_note", length = 500)
    private String closeReasonNote;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    /**
     * 낙관적 락 버전 (`.ai/docs/global/CONCURRENCY.md`).
     *
     * <p>⛔ {@code @Version} 을 붙이지 마라. {@code ProjectMapper.toEntity} 가 매번 {@code new} 로
     * detached 객체를 만들어 JPA 가 {@code merge} 로 처리하는데, merge 는 DB 의 최신 version 을
     * 다시 읽어와 검사하므로 <b>항상 통과한다</b> (§6-1).
     */
    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "created_by", nullable = false, length = 20)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}