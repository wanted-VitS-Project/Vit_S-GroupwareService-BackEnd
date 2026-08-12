package com.group3.vitamins.bidding.referencefile.infrastructure.persistence.adapter;

import com.group3.vitamins.bidding.bidreview.domain.model.BidReviewStatus;
import com.group3.vitamins.bidding.referencefile.application.port.BidReferenceFileActiveUsagePort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class BidReferenceFileActiveUsageAdapter implements BidReferenceFileActiveUsagePort {

    // BidReviewStatus.isProcessing()과 같은 뜻 — 상태값이 바뀌면 이 어댑터도 같이 어긋나지 않도록
    // 여기서도 enum 상수를 그대로 참조한다(문자열 리터럴 직접 나열 금지).
    private static final List<String> ACTIVE_REVIEW_STATUSES = List.of(
            BidReviewStatus.PENDING.name(),
            BidReviewStatus.PROCESSING.name()
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public boolean existsActiveReviewUsage(Long companyId, Long referenceFileId) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM bid_review_document document
                    INNER JOIN bid_review review
                        ON review.bid_review_id = document.bid_review_id
                    WHERE review.company_id = :companyId
                      AND document.bid_reference_file_id = :referenceFileId
                      AND review.review_status IN (:activeStatuses)
                )
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("companyId", companyId)
                .addValue("referenceFileId", referenceFileId)
                .addValue("activeStatuses", ACTIVE_REVIEW_STATUSES);

        Boolean exists = jdbcTemplate.queryForObject(sql, parameters, Boolean.class);
        return Boolean.TRUE.equals(exists);
    }
}