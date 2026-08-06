package com.group3.vitamins.file.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * {@code block_file} 테이블 매핑 — 블록 ↔ 파일 연결 (파일 소유, `BLOCK.md` §4-4).
 *
 * <p>⛔ {@code block_id} 에는 FK 가 없다(다형성 역방향). {@code file_id} → file 은 FK + {@code ON DELETE CASCADE}.
 * ⛔ 순수 연결 행이라 {@code deleted_at} 이 없다(hard delete) — 파일 영구삭제 시 CASCADE 로 지워진다(§8-1).
 * {@code linked_at} 은 DB 기본값이 관리하므로 매핑하지 않는다.
 */
@Entity
@Table(name = "block_file")
@IdClass(BlockFileId.class)
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BlockFileJpaEntity {

    @Id
    @Column(name = "block_id")
    private Long blockId;

    @Id
    @Column(name = "file_id")
    private Long fileId;

    @Column(name = "linked_by", nullable = false, length = 20)
    private String linkedBy;
}
