package com.group3.vitamins.employeegroup.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 그룹-구성원 매핑 JPA 엔티티. 복합 PK (group_id, user_id). {@code created_at}(추가일)은 DB 기본값이라
 * insert 에 넣지 않고 읽기 전용으로만 매핑한다. FK 는 CASCADE(그룹·사원 삭제 시 함께 제거).
 */
@Entity
@Table(name = "employee_group_member")
@IdClass(EmployeeGroupMemberId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmployeeGroupMemberJpaEntity {

    @Id
    @Column(name = "group_id")
    private Long groupId;

    @Id
    @Column(name = "user_id", length = 20)
    private String userId;

    public EmployeeGroupMemberJpaEntity(Long groupId, String userId) {
        this.groupId = groupId;
        this.userId = userId;
    }
}
