package com.group3.vitamins.bidding.collectionrun.application.port;

import com.group3.vitamins.bidding.collectionrun.application.model.CollectedBidNoticePayload;

import java.time.LocalDateTime;
import java.util.List;

public interface CollectedBidNoticeStorePort {

    // 한 페이지에서 수집한 공고와 원문을 같은 저장 흐름으로 반영합니다.
    StoreResult saveAll(
            String sourceCode,
            Long runId,
            List<CollectedBidNoticePayload> payloads,
            LocalDateTime crawledAt
    );

    record StoreResult(
            int insertedCount,
            int updatedCount,
            int skippedCount
    ) {

        public StoreResult {
            if (insertedCount < 0 || updatedCount < 0 || skippedCount < 0) {
                throw new IllegalArgumentException(
                        "store result counts must not be negative"
                );
            }
        }

        // 이번 저장 작업에서 처리한 전체 공고 수를 반환합니다.
        public int totalCount() {
            return insertedCount + updatedCount + skippedCount;
        }
    }
}