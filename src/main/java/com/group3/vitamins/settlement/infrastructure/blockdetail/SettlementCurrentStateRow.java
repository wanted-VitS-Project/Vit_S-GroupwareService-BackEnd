package com.group3.vitamins.settlement.infrastructure.blockdetail;

import java.time.LocalDateTime;

/**
 * lockSiblingSettlementBlocksForUpdate 직후, FOR UPDATE로 다시 읽은 이 settleId 행의 현재 상태.
 *
 * @param version 현재(최신 커밋) version
 * @param status 현재 status
 * @param deletedAt 삭제 시각. 삭제 안 됐으면 null
 */
public record SettlementCurrentStateRow(Integer version, String status, LocalDateTime deletedAt) {
}
