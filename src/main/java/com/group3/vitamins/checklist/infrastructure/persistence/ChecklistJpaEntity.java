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
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * {@code @DynamicUpdate} — 바뀐 컬럼만 UPDATE 문에 넣는다 (텍스트 도메인과 동일한 이유:
 * 오래된 deletedAt 값을 실어 보내 동시 삭제를 되살리는 것을 막는다).
 */
@Entity
@NoArgsConstructor
@Getter
@DynamicUpdate
@Table(name = "checklist")
public class ChecklistJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chk_id")
    private Long chkId;

    // FK 아님(엔티티 매핑만 FK). 소속 체크리스트 블록(checklist_block.chk_block_id) 참조값.
    @Column(name = "chk_block_id", nullable = false)
    private Long chkBlockId;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "is_completed", nullable = false)
    private boolean completed;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public ChecklistJpaEntity(Long chkBlockId, String content) {
        this.chkBlockId = chkBlockId;
        this.content = content;
        this.completed = false;
    }
}
