package com.group3.vitamins.employee.contract;

import com.group3.vitamins.global.domain.event.DomainEvent;

import java.util.Objects;

/** 사원이 업무에 참여할 수 있는 상태에서 퇴사·삭제·계정 비활성 상태로 바뀌었음을 알리는 공용 계약. */
public record EmployeeParticipationUnavailableEvent(
        String userId,
        Long companyId
) implements DomainEvent {

    public EmployeeParticipationUnavailableEvent {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(companyId, "companyId must not be null");
        if (userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
    }
}
