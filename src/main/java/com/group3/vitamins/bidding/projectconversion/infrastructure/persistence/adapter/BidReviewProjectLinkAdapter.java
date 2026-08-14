package com.group3.vitamins.bidding.projectconversion.infrastructure.persistence.adapter;

import com.group3.vitamins.bidding.projectconversion.application.port.BidReviewProjectLinkPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BidReviewProjectLinkAdapter implements BidReviewProjectLinkPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Optional<ReviewSnapshot> findReview(Long reviewId) {
        String sql = """
                SELECT bid_review_id, company_id, bid_notice_id, requested_by, review_status, project_id
                FROM bid_review
                WHERE bid_review_id = :reviewId
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("reviewId", reviewId);

        return jdbcTemplate.query(
                sql,
                parameters,
                (resultSet, rowNumber) -> new ReviewSnapshot(
                        resultSet.getLong("bid_review_id"),
                        resultSet.getLong("company_id"),
                        resultSet.getLong("bid_notice_id"),
                        resultSet.getString("requested_by"),
                        resultSet.getString("review_status"),
                        resultSet.getObject("project_id") == null ? null : resultSet.getLong("project_id")
                )
        ).stream().findFirst();
    }
}
