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

    // findHistory·countHistory 둘 다 같은 대상 행을 스코프한다 - 둘 중 하나만 바꾸고 잊어버리지 않게
    // WHERE 절을 상수로 뽑아 공유한다(MyBatis의 <sql> 조각과 같은 역할, 이 어댑터는 raw JDBC라 대신 이렇게 처리).
    private static final String HISTORY_SCOPE_WHERE = """
            WHERE review.company_id = :companyId
              AND review.bid_notice_id = :noticeId
              AND review.requested_by = :userId
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<HistoryRow> findHistory(Long companyId, Long noticeId, String userId, int offset, int size) {
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
                """ + HISTORY_SCOPE_WHERE + """
                ORDER BY review.created_at DESC, review.bid_review_id DESC
                LIMIT :size OFFSET :offset
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("companyId", companyId)
                .addValue("noticeId", noticeId)
                .addValue("userId", userId)
                .addValue("size", size)
                .addValue("offset", offset);

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

    @Override
    public long countHistory(Long companyId, Long noticeId, String userId) {
        String sql = "SELECT COUNT(*) FROM bid_review review " + HISTORY_SCOPE_WHERE;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("companyId", companyId)
                .addValue("noticeId", noticeId)
                .addValue("userId", userId);

        Long count = jdbcTemplate.queryForObject(sql, parameters, Long.class);
        return count == null ? 0L : count;
    }
}