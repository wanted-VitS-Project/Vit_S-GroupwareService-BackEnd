package com.group3.vitamins.bidding.projectconversion.infrastructure.persistence.adapter;

import com.group3.vitamins.bidding.projectconversion.application.port.BidReviewProjectLinkPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
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

    @Override
    public List<PromotableDocument> findPromotableDocuments(Long reviewId) {
        String sql = """
                SELECT bid_review_document_id, temporary_storage_key, file_name, file_size
                FROM bid_review_document
                WHERE bid_review_id = :reviewId
                  AND document_role = 'BID_ATTACHMENT'
                  AND processing_status = 'READY'
                  AND deleted_at IS NULL
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("reviewId", reviewId);

        return jdbcTemplate.query(
                sql,
                parameters,
                (resultSet, rowNumber) -> new PromotableDocument(
                        resultSet.getLong("bid_review_document_id"),
                        resultSet.getString("temporary_storage_key"),
                        resultSet.getString("file_name"),
                        resultSet.getLong("file_size")
                )
        );
    }

    @Override
    public boolean markDocumentPromoted(Long reviewDocumentId, Long fileId, Long fileVersionId, LocalDateTime now) {
        String sql = """
                UPDATE bid_review_document
                SET processing_status = 'PROMOTED',
                    promoted_file_id = :fileId,
                    promoted_file_version_id = :fileVersionId,
                    promoted_at = :now,
                    updated_at = :now
                WHERE bid_review_document_id = :reviewDocumentId
                  AND document_role = 'BID_ATTACHMENT'
                  AND processing_status = 'READY'
                  AND deleted_at IS NULL
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("fileId", fileId)
                .addValue("fileVersionId", fileVersionId)
                .addValue("now", now)
                .addValue("reviewDocumentId", reviewDocumentId);

        return jdbcTemplate.update(sql, parameters) > 0;
    }

    @Override
    public boolean linkProject(Long reviewId, Long projectId, LocalDateTime now) {
        String sql = """
                UPDATE bid_review
                SET project_id = :projectId,
                    updated_at = :now
                WHERE bid_review_id = :reviewId
                  AND review_status = 'COMPLETED'
                  AND project_id IS NULL
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("projectId", projectId)
                .addValue("now", now)
                .addValue("reviewId", reviewId);

        return jdbcTemplate.update(sql, parameters) > 0;
    }
}
