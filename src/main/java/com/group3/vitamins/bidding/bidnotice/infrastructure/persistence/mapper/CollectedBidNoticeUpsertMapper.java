package com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.mapper;

import com.group3.vitamins.bidding.collectionrun.application.model.CollectedBidNotice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface CollectedBidNoticeUpsertMapper {

    // 이 배치에 포함된 (external_id, notice_ord) 조합 중 이미 존재하는 공고의 ID를 한 번에 조회합니다.
    List<NoticeIdRow> findNoticeIds(
            @Param("sourceId") Long sourceId,
            @Param("notices") List<CollectedBidNotice> notices
    );

    // 배치 전체를 한 번의 INSERT ... ON DUPLICATE KEY UPDATE로 신규 삽입 또는 갱신합니다.
    int upsertNotices(
            @Param("sourceId") Long sourceId,
            @Param("rows") List<NoticeUpsertRow> rows,
            @Param("crawledAt") LocalDateTime crawledAt
    );

    // 이 배치의 (bid_notice_id, raw_payload_hash) 중 이미 기록된 원문을 한 번에 조회합니다.
    List<RawKeyPair> findExistingRawKeys(
            @Param("keys") List<RawKeyPair> keys
    );

    // 새로 확인된 원문만 한 번에 삽입합니다. UNIQUE(bid_notice_id, raw_payload_hash) 위반은 조용히 무시합니다.
    int insertRawRecords(
            @Param("records") List<RawRecordKey> records,
            @Param("runId") Long runId,
            @Param("sourceCode") String sourceCode,
            @Param("parsedAt") LocalDateTime parsedAt
    );

    // 원문이 이미 기록돼 있던 공고들의 관측 시각만 한 번에 갱신합니다.
    int touchObservedAtBulk(
            @Param("noticeIds") Collection<Long> noticeIds,
            @Param("crawledAt") LocalDateTime crawledAt
    );
}
