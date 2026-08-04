package com.group3.vitamins.checklist.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 체크리스트 블록 상세 행({@code checklist_block}) — 생성·삭제는 Block 도메인(동훈님)이 전담한다.
 * 이 도메인은 항목 생성 시 대상 블록의 존재/활성 여부를 확인하는 읽기 전용 용도로만 매핑한다.
 */
@Entity
@NoArgsConstructor
@Getter
@Table(name = "checklist_block")
public class ChecklistBlockJpaEntity {

    @Id
    @Column(name = "chk_block_id")
    private Long chkBlockId;

    // FK 아님. 공용 block 테이블 참조용 값만 저장 (동훈님 쪽에서 채워줌)
    @Column(name = "block_id", nullable = false)
    private Long blockId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
