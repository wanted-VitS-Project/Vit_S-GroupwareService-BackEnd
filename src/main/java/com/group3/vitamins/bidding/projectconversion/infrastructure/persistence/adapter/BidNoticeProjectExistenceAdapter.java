package com.group3.vitamins.bidding.projectconversion.infrastructure.persistence.adapter;

import com.group3.vitamins.bidding.projectconversion.application.port.BidNoticeProjectExistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BidNoticeProjectExistenceAdapter implements BidNoticeProjectExistencePort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public boolean existsForNotice(Long companyId, Long noticeId) {
        String sql = """
                SELECT 1
                FROM project
                WHERE bid_notice_id = :noticeId
                  AND company_id = :companyId
                  AND deleted_at IS NULL
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("noticeId", noticeId)
                .addValue("companyId", companyId);

        return !jdbcTemplate.queryForList(sql, parameters).isEmpty();
    }
}
