package com.group3.vitamins.bidding.bidreview.infrastructure.persistence.adapter;

import com.group3.vitamins.bidding.bidreview.application.port.BidReviewNoticeDocumentPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BidReviewNoticeDocumentAdapter
        implements BidReviewNoticeDocumentPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Optional<NoticeSnapshot> findAccessibleNotice(
            Long companyId,
            Long noticeId
    ) {
        String sql = """
                SELECT
                    notice.bid_notice_id,
                    notice.notice_name
                FROM bid_notice notice
                INNER JOIN company_bid_notice_state state
                    ON state.bid_notice_id = notice.bid_notice_id
                   AND state.company_id = :companyId
                WHERE notice.bid_notice_id = :noticeId
                  AND notice.deleted_at IS NULL
                  AND state.notice_status <> 'DISMISSED'
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("companyId", companyId)
                .addValue("noticeId", noticeId);

        return jdbcTemplate.query(
                sql,
                parameters,
                (resultSet, rowNumber) -> new NoticeSnapshot(
                        resultSet.getLong("bid_notice_id"),
                        resultSet.getString("notice_name")
                )
        ).stream().findFirst();
    }

    @Override
    public List<AttachmentSnapshot> findAttachments(
            Long companyId,
            Long noticeId,
            List<Long> attachmentIds
    ) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return List.of();
        }

        String sql = """
                SELECT
                    attachment.bid_notice_attachment_id,
                    attachment.bid_notice_id,
                    attachment.file_name,
                    attachment.source_url
                FROM bid_notice_attachment attachment
                INNER JOIN bid_notice notice
                    ON notice.bid_notice_id = attachment.bid_notice_id
                INNER JOIN company_bid_notice_state state
                    ON state.bid_notice_id = notice.bid_notice_id
                   AND state.company_id = :companyId
                WHERE attachment.bid_notice_id = :noticeId
                  AND attachment.bid_notice_attachment_id IN (:attachmentIds)
                  AND attachment.deleted_at IS NULL
                  AND notice.deleted_at IS NULL
                  AND state.notice_status <> 'DISMISSED'
                ORDER BY
                    attachment.attachment_order ASC,
                    attachment.bid_notice_attachment_id ASC
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("companyId", companyId)
                .addValue("noticeId", noticeId)
                .addValue("attachmentIds", attachmentIds);

        return jdbcTemplate.query(
                sql,
                parameters,
                (resultSet, rowNumber) -> new AttachmentSnapshot(
                        resultSet.getLong("bid_notice_attachment_id"),
                        resultSet.getLong("bid_notice_id"),
                        resultSet.getString("file_name"),
                        resultSet.getString("source_url")
                )
        );
    }
}