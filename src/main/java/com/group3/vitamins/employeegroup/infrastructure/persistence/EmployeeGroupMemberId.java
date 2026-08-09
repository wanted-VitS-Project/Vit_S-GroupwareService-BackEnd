package com.group3.vitamins.employeegroup.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;

/** {@code employee_group_member} 복합 PK (group_id, user_id). @IdClass 대상. */
public class EmployeeGroupMemberId implements Serializable {

    private Long groupId;
    private String userId;

    public EmployeeGroupMemberId() {
    }

    public EmployeeGroupMemberId(Long groupId, String userId) {
        this.groupId = groupId;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EmployeeGroupMemberId that)) {
            return false;
        }
        return Objects.equals(groupId, that.groupId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, userId);
    }
}
