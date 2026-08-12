package com.group3.vitamins.bidding.bidreview.infrastructure.persistence.adapter;

import com.group3.vitamins.bidding.bidreview.application.port.BidReviewHistoryQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class BidReviewHistoryQueryAdapter implements BidReviewHistoryQueryPort {

    private static final int MAX_HISTORY_SIZE = 20;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<HistoryRow> findHistory(Long companyId, Long noticeId, String userId) {
        String sql = """
                SELECT
                    review.bid_review_id,
                    review.review_status,
                    review.prompt,
                    review.created_at,
                    review.completed_at,
                    review.expires_at,
                    review.project_id
                FROM bid_review review
                WHERE review.company_id = :companyId
                  AND review.bid_notice_id = :noticeId
                  AND review.requested_by = :userId
                ORDER BY review.created_at DESC
                LIMIT :maxSize
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("companyId", companyId)
                .addValue("noticeId", noticeId)
                .addValue("userId", userId)
                .addValue("maxSize", MAX_HISTORY_SIZE);

        return jdbcTemplate.query(
                sql,
                parameters,
                (resultSet, rowNumber) -> new HistoryRow(
                        resultSet.getLong("bid_review_id"),
                        resultSet.getString("review_status"),
                        resultSet.getString("prompt"),
                        resultSet.getObject("created_at", LocalDateTime.class),
                        resultSet.getObject("completed_at", LocalDateTime.class),
                        resultSet.getObject("expires_at", LocalDateTime.class),
                        resultSet.getObject("project_id", Long.class)
                )
        );
    }
}