package com.group3.vitamins.pagepermission.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 페이지 권한 JPA 엔티티 (`page_permission` — init 마이그레이션에 이미 존재). 부여 대상(BIDDING·FINANCE) 행만 저장된다.
 *
 * <p>{@code created_at}·{@code updated_at} 은 DB 가 채우고 응답 집계는 MyBatis 가 읽으므로 매핑하지 않는다
 * (매핑하지 않은 컬럼은 {@code ddl-auto: validate} 에 걸리지 않는다). {@code permission} 은 계정 role 처럼 ENUM 이다.
 */
@Entity
@Table(name = "page_permission",
        uniqueConstraints = @UniqueConstraint(name = "uk_page_permission", columnNames = {"page_code", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PagePermissionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "page_permission_id")
    private Long pagePermissionId;

    @Column(name = "page_code", nullable = false, length = 50)
    private String pageCode;

    @Column(name = "user_id", nullable = false, length = 20)
    private String userId;

    @Column(name = "permission", nullable = false, columnDefinition = "enum('VIEWER','EDITOR')")
    private String permission;

    public PagePermissionJpaEntity(String pageCode, String userId, String permission) {
        this.pageCode = pageCode;
        this.userId = userId;
        this.permission = permission;
    }

    /** 등급 변경(§4 부여와 같은 API 에서 기존 행을 갱신). */
    public void changePermission(String permission) {
        this.permission = permission;
    }
}
