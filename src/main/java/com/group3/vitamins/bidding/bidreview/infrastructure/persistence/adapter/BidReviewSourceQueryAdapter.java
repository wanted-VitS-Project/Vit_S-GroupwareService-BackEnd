package com.group3.vitamins.bidding.bidreview.infrastructure.persistence.adapter;

import com.group3.vitamins.bidding.bidreview.application.port.BidReviewSourceQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class BidReviewSourceQueryAdapter implements BidReviewSourceQueryPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<AttachmentSource> findAttachmentSources(Long companyId, Long noticeId) {
        String sql = """
                SELECT
                    attachment.bid_notice_attachment_id,
                    attachment.file_name,
                    crawl_source.source_type
                FROM bid_notice_attachment attachment
                INNER JOIN bid_notice notice
                    ON notice.bid_notice_id = attachment.bid_notice_id
                INNER JOIN crawl_source crawl_source
                    ON crawl_source.crawl_source_id = notice.crawl_source_id
                INNER JOIN company_bid_notice_state state
                    ON state.bid_notice_id = notice.bid_notice_id
                   AND state.company_id = :companyId
                WHERE attachment.bid_notice_id = :noticeId
                  AND attachment.deleted_at IS NULL
                  AND notice.deleted_at IS NULL
                  AND state.notice_status <> 'DISMISSED'
                ORDER BY
                    attachment.attachment_order ASC,
                    attachment.bid_notice_attachment_id ASC
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("companyId", companyId)
                .addValue("noticeId", noticeId);

        return jdbcTemplate.query(
                sql,
                parameters,
                (resultSet, rowNumber) -> new AttachmentSource(
                        resultSet.getLong("bid_notice_attachment_id"),
                        resultSet.getString("file_name"),
                        resultSet.getString("source_type")
                )
        );
    }
}   