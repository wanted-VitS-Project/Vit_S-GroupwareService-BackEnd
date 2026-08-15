package com.group3.vitamins.bidding.collectionrun.presentation.api.request;

import com.group3.vitamins.bidding.collectionrun.application.command.StartCollectionRunCommand;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record StartCollectionRunRequest(

        @Schema(
                description = "수동 조회 시작 시각(선택). endedAt과 함께 지정해야 하며, "
                        + "생략하면 조건에 저장된 lookbackPeriod로 자동 계산한다",
                example = "2026-07-01T00:00:00"
        )
        LocalDateTime startedAt,

        @Schema(
                description = "수동 조회 종료 시각(선택). startedAt과 함께 지정해야 한다",
                example = "2026-08-01T00:00:00"
        )
        LocalDateTime endedAt
) {

    // 요청 본문이 없는 경우에도 쓸 수 있는 빈 요청입니다.
    public static final StartCollectionRunRequest EMPTY =
            new StartCollectionRunRequest(null, null);

    public StartCollectionRunCommand toCommand(Long conditionId, String userId) {
        return new StartCollectionRunCommand(conditionId, userId, startedAt, endedAt);
    }
}
