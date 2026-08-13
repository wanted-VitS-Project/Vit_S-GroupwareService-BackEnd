package com.group3.vitamins.project.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 프로젝트 복제 요청 (PRJ-018).
 *
 * <p>{@code sourceProjectId} 를 뺀 나머지는 {@link CreateProjectCommand} 와 같다 —
 * 복제는 "프로젝트 생성 + 구조 복사" 이고, <b>원본의 필드값은 하나도 승계하지 않는다.</b>
 */
public record DuplicateProjectCommand(
        Long sourceProjectId,
        Long bidNoticeId,
        String name,
        String description,
        String clientName,
        LocalDate startedOn,
        LocalDate endedOn,
        BigDecimal contractAmount,
        List<Long> businessCategoryIds,
        String requesterUserId,
        String role
) {

    /**
     * 복제본 생성에 쓸 커맨드로 옮긴다.
     *
     * <p>⚠️ {@code bidNoticeId} 는 <b>요청에 담긴 값만</b> 넘어간다. 원본의 공고를 승계하면
     * {@code uk_project_bid_notice} UNIQUE 위반(1062)으로 복제가 통째로 실패한다.
     */
    public CreateProjectCommand toCreateCommand() {
        return new CreateProjectCommand(bidNoticeId, name, description, clientName,
                startedOn, endedOn, contractAmount, businessCategoryIds, requesterUserId);
    }
}
