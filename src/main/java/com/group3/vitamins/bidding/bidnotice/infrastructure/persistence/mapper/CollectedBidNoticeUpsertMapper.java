package com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.mapper;

import com.group3.vitamins.bidding.collectionrun.application.model.CollectedBidNotice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface CollectedBidNoticeUpsertMapper {

    int insertNoticeIfAbsent(
            @Param("sourceId") Long sourceId,
            @Param("notice") CollectedBidNotice notice,
            @Param("hasAttachments") boolean hasAttachments,
            @Param("crawledAt") LocalDateTime crawledAt
    );

    Long findNoticeId(
            @Param("sourceId") Long sourceId,
            @Param("externalId") String externalId,
            @Param("noticeOrder") String noticeOrder
    );

    int updateCollectedNotice(
            @Param("noticeId") Long noticeId,
            @Param("notice") CollectedBidNotice notice,
            @Param("hasAttachments") boolean hasAttachments,
            @Param("crawledAt") LocalDateTime crawledAt
    );

    int touchObservedAt(
            @Param("noticeId") Long noticeId,
            @Param("crawledAt") LocalDateTime crawledAt
    );

    int insertRawIfAbsent(
            @Param("noticeId") Long noticeId,
            @Param("runId") Long runId,
            @Param("sourceCode") String sourceCode,
            @Param("rawPayload") String rawPayload,
            @Param("rawPayloadHash") String rawPayloadHash,
            @Param("parsedAt") LocalDateTime parsedAt
    );
}
