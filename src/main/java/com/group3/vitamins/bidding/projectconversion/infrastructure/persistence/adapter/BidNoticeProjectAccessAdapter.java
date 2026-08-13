package com.group3.vitamins.bidding.projectconversion.infrastructure.persistence.adapter;

import com.group3.vitamins.bidding.projectconversion.application.port.BidNoticeProjectAccessPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BidNoticeProjectAccessAdapter implements BidNoticeProjectAccessPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public boolean isAccessible(Long companyId, Long noticeId) {
        String sql = """
                SELECT 1
                FROM bid_notice notice
                INNER JOIN company_bid_notice_state state
                    ON state.bid_notice_id = notice.bid_notice_id
                   AND state.company_id = :companyId
                WHERE notice.bid_notice_id = :noticeId
                  AND notice.deleted_at IS NULL
                  AND state.notice_status <> 'DISMISSED'
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("companyId", companyId)
                .addValue("noticeId", noticeId);

        return !jdbcTemplate.queryForList(sql, parameters).isEmpty();
    }
}
