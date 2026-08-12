package com.group3.vitamins.bidding.referencefile.infrastructure.persistence.adapter;

import com.group3.vitamins.bidding.referencefile.application.port.BidReferenceFileActiveUsagePort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BidReferenceFileActiveUsageAdapter implements BidReferenceFileActiveUsagePort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public boolean existsActiveReviewUsage(Long companyId, Long referenceFileId) {
        String sql = """
                SELECT COUNT(*)
                FROM bid_review_document document
                INNER JOIN bid_review review
                    ON review.bid_review_id = document.bid_review_id
                WHERE review.company_id = :companyId
                  AND document.bid_reference_file_id = :referenceFileId
                  AND review.review_status IN ('PENDING', 'PROCESSING')
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("companyId", companyId)
                .addValue("referenceFileId", referenceFileId);

        Integer count = jdbcTemplate.queryForObject(sql, parameters, Integer.class);
        return count != null && count > 0;
    }
}