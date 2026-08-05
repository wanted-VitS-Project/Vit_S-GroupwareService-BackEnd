package com.group3.vitamins.checklist.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 체크리스트 블록 상세 행({@code checklist_block}) — 만들 시점은 Block 도메인(동훈님)이 판단하고
 * 실제 INSERT·삭제는 이 도메인이 한다. 항목 생성 시 대상 블록의 존재/활성 여부 확인에도 쓰인다.
 */
@Entity
@NoArgsConstructor
@Getter
@Table(name = "checklist_block")
public class ChecklistBlockJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chk_block_id")
    private Long chkBlockId;

    // FK 아님. 공용 block 테이블 참조용 값만 저장 (동훈님 쪽에서 채워줌)
    @Column(name = "block_id", nullable = false)
    private Long blockId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** 상세 빈 행 생성용. 항목(checklist)은 0개로 시작한다. */
    public ChecklistBlockJpaEntity(Long blockId) {
        this.blockId = blockId;
    }
}
