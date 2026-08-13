package com.group3.vitamins.bidding.bidreview.infrastructure.persistence.adapter;

import com.group3.vitamins.bidding.bidreview.application.port.BidReviewReferenceFilePort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class BidReviewReferenceFileAdapter
        implements BidReviewReferenceFilePort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<ReferenceFileSnapshot> findAccessibleFiles(
            Long companyId,
            List<Long> referenceFileIds
    ) {
        if (referenceFileIds == null || referenceFileIds.isEmpty()) {
            return List.of();
        }

        // FOR UPDATE로 잠가, 이 검토 생성 트랜잭션이 끝날 때까지 해당 파일의 삭제를 막는다
        // (BidReferenceFileRepository.findActiveByIdAndCompanyIdForDeletion과 같은 행을 잠근다).
        String sql = """
                SELECT
                    file.bid_reference_file_id,
                    file.file_name,
                    file.upload_status,
                    file.index_status
                FROM bid_reference_file file
                WHERE file.company_id = :companyId
                  AND file.bid_reference_file_id IN (:referenceFileIds)
                  AND file.deleted_at IS NULL
                FOR UPDATE
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("companyId", companyId)
                .addValue("referenceFileIds", referenceFileIds);

        return jdbcTemplate.query(
                sql,
                parameters,
                (resultSet, rowNumber) -> new ReferenceFileSnapshot(
                        resultSet.getLong("bid_reference_file_id"),
                        resultSet.getString("file_name"),
                        resultSet.getString("upload_status"),
                        resultSet.getString("index_status")
                )
        );
    }
}
