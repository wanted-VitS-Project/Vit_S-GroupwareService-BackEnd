package com.group3.vitamins.project.application.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ProjectDetailResult(
        Long projectId,
        String name,
        String description,
        String clientName,
        String status,
        LocalDate startedOn,
        LocalDate endedOn,
        BigDecimal contractAmount,
        Integer progressRate,
        int stepCount,
        int doneStepCount,
        List<BusinessCategorySummary> businessCategories,
        Long bidNoticeId,
        String closeReasonCode,
        String closeReasonNote,
        String myPermission,
        LocalDateTime createdAt,

        /**
         * 🚨 <b>빠뜨리면 안 된다.</b> 프론트가 수정·상태변경 요청에 실어 보낼 값이 여기서만 나온다.
         * 없으면 0/null 을 보내 <b>모든 저장이 409</b> 가 되는데 컴파일도 테스트도 통과한다
         * (`CONCURRENCY.md` §6-3).
         */
        int version
) {
}