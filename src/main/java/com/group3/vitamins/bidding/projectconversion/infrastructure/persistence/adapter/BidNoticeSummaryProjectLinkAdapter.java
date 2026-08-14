package com.group3.vitamins.bidding.projectconversion.infrastructure.persistence.adapter;

import com.group3.vitamins.bidding.projectconversion.application.port.BidNoticeSummaryProjectLinkPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BidNoticeSummaryProjectLinkAdapter implements BidNoticeSummaryProjectLinkPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Optional<SummarySnapshot> findSummary(Long companyId, Long noticeId, Long summaryId) {
        String sql = """
                SELECT bid_notice_summary_id, confirmed, project_id
                FROM bid_notice_summary
                WHERE bid_notice_summary_id = :summaryId
                  AND company_id = :companyId
                  AND bid_notice_id = :noticeId
                  AND deleted_at IS NULL
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("summaryId", summaryId)
                .addValue("companyId", companyId)
                .addValue("noticeId", noticeId);

        return jdbcTemplate.query(
                sql,
                parameters,
                (resultSet, rowNumber) -> new SummarySnapshot(
                        resultSet.getLong("bid_notice_summary_id"),
                        resultSet.getBoolean("confirmed"),
                        resultSet.getObject("project_id") == null ? null : resultSet.getLong("project_id")
                )
        ).stream().findFirst();
    }

    @Override
    public boolean linkProject(Long companyId, Long noticeId, Long summaryId, Long projectId, LocalDateTime now) {
        String sql = """
                UPDATE bid_notice_summary
                SET project_id = :projectId,
                    updated_at = :now
                WHERE bid_notice_summary_id = :summaryId
                  AND company_id = :companyId
                  AND bid_notice_id = :noticeId
                  AND deleted_at IS NULL
                  AND project_id IS NULL
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("projectId", projectId)
                .addValue("now", now)
                .addValue("summaryId", summaryId)
                .addValue("companyId", companyId)
                .addValue("noticeId", noticeId);

        return jdbcTemplate.update(sql, parameters) > 0;
    }
}
