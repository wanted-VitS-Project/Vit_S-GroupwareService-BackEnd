package com.group3.vitamins.bidding.bidreview.infrastructure.persistence.adapter;

import com.group3.vitamins.bidding.bidreview.application.port.BidReviewDetailQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BidReviewDetailQueryAdapter implements BidReviewDetailQueryPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Optional<ReviewRow> findReview(Long reviewId) {
        String sql = """
                SELECT
                    review.bid_review_id,
                    review.company_id,
                    review.bid_notice_id,
                    review.requested_by,
                    review.prompt,
                    review.review_status,
                    review.result,
                    review.error_message,
                    review.created_at,
                    review.completed_at,
                    review.expires_at,
                    review.project_id
                FROM bid_review review
                WHERE review.bid_review_id = :reviewId
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("reviewId", reviewId);

        return jdbcTemplate.query(
                sql,
                parameters,
                (resultSet, rowNumber) -> new ReviewRow(
                        resultSet.getLong("bid_review_id"),
                        resultSet.getLong("company_id"),
                        resultSet.getLong("bid_notice_id"),
                        resultSet.getString("requested_by"),
                        resultSet.getString("prompt"),
                        resultSet.getString("review_status"),
                        resultSet.getString("result"),
                        resultSet.getString("error_message"),
                        resultSet.getObject("created_at", LocalDateTime.class),
                        resultSet.getObject("completed_at", LocalDateTime.class),
                        resultSet.getObject("expires_at", LocalDateTime.class),
                        resultSet.getObject("project_id", Long.class)
                )
        ).stream().findFirst();
    }

    @Override
    public List<DocumentRow> findDocuments(Long reviewId) {
        String sql = """
                SELECT
                    document.document_role,
                    document.bid_notice_attachment_id,
                    document.bid_reference_file_id,
                    document.company_document_version_id,
                    document.file_name,
                    document.processing_status
                FROM bid_review_document document
                WHERE document.bid_review_id = :reviewId
                ORDER BY
                    document.document_role ASC,
                    document.bid_review_document_id ASC
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("reviewId", reviewId);

        return jdbcTemplate.query(
                sql,
                parameters,
                (resultSet, rowNumber) -> new DocumentRow(
                        resultSet.getString("document_role"),
                        resultSet.getObject("bid_notice_attachment_id", Long.class),
                        resultSet.getObject("bid_reference_file_id", Long.class),
                        resultSet.getObject("company_document_version_id", Long.class),
                        resultSet.getString("file_name"),
                        resultSet.getString("processing_status")
                )
        );
    }

    @Override
    public List<CitationRow> findCitations(Long reviewId) {
        String sql = """
                SELECT
                    citation.rank_order,
                    document.document_role,
                    document.bid_notice_attachment_id,
                    document.bid_reference_file_id,
                    document.company_document_version_id,
                    citation.file_name,
                    citation.page_number,
                    citation.sheet_name,
                    citation.excerpt
                FROM bid_review_citation citation
                INNER JOIN bid_review_document document
                    ON document.bid_review_document_id = citation.bid_review_document_id
                WHERE citation.bid_review_id = :reviewId
                ORDER BY citation.rank_order ASC
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("reviewId", reviewId);

        return jdbcTemplate.query(
                sql,
                parameters,
                (resultSet, rowNumber) -> new CitationRow(
                        resultSet.getInt("rank_order"),
                        resultSet.getString("document_role"),
                        resultSet.getObject("bid_notice_attachment_id", Long.class),
                        resultSet.getObject("bid_reference_file_id", Long.class),
                        resultSet.getObject("company_document_version_id", Long.class),
                        resultSet.getString("file_name"),
                        resultSet.getObject("page_number", Integer.class),
                        resultSet.getString("sheet_name"),
                        resultSet.getString("excerpt")
                )
        );
    }
}