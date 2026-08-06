package com.group3.vitamins.file.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * {@code file} 테이블 매핑 (논리 문서).
 *
 * <p>{@code created_at}·{@code updated_at} 은 DB 기본값이 관리하므로 매핑하지 않는다(job_position 선례).
 * {@code deleted_at} 은 휴지통 이동(§5)에서 갱신하므로 매핑한다.
 */
@Entity
@Table(name = "file")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long fileId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "created_by", nullable = false, length = 20)
    private String createdBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
