package com.group3.vitamins.bidding.bidreview.infrastructure.persistence.adapter;

import com.group3.vitamins.bidding.bidreview.application.port.BidReviewReferenceFilePort;
import com.group3.vitamins.file.application.port.FileStoragePort;
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
    private final FileStoragePort fileStoragePort;

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

    @Override
    public List<DownloadableReferenceFile> findDownloadableFiles(
            Long companyId,
            List<Long> referenceFileIds
    ) {
        if (referenceFileIds == null || referenceFileIds.isEmpty()) {
            return List.of();
        }

        // Worker 작업 조회 전용 — 여기는 잠글 필요 없다(검토는 이미 생성돼 처리 중인 상태).
        String sql = """
                SELECT
                    file.bid_reference_file_id,
                    file.file_name,
                    file.storage_key
                FROM bid_reference_file file
                WHERE file.company_id = :companyId
                  AND file.bid_reference_file_id IN (:referenceFileIds)
                  AND file.deleted_at IS NULL
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("companyId", companyId)
                .addValue("referenceFileIds", referenceFileIds);

        return jdbcTemplate.query(
                sql,
                parameters,
                (resultSet, rowNumber) -> {
                    String fileName = resultSet.getString("file_name");
                    String storageKey = resultSet.getString("storage_key");

                    return new DownloadableReferenceFile(
                            resultSet.getLong("bid_reference_file_id"),
                            fileName,
                            fileStoragePort.presignDownload(storageKey, fileName).url()
                    );
                }
        );
    }
}
