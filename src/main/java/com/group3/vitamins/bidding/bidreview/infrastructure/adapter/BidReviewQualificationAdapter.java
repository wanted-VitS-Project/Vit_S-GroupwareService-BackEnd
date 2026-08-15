package com.group3.vitamins.bidding.bidreview.infrastructure.adapter;

import com.group3.vitamins.bidding.bidreview.application.port.BidReviewQualificationPort;
import com.group3.vitamins.employee.domain.model.Degree;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class BidReviewQualificationAdapter implements BidReviewQualificationPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<NameCount> summarizeMajors(Long companyId) {
        String sql = """
                SELECT m.name AS name, COUNT(DISTINCT ee.user_id) AS headcount
                FROM employee_education ee
                JOIN employee e ON e.user_id = ee.user_id
                JOIN major m ON m.major_id = ee.major_id
                WHERE ee.company_id = :companyId
                  AND e.deleted_at IS NULL
                  AND e.resigned_at IS NULL
                  AND e.is_system = 0
                GROUP BY m.major_id, m.name
                ORDER BY m.name ASC
                """;
        return query(sql, companyId, resultSet -> resultSet.getString("name"));
    }

    @Override
    public List<NameCount> summarizeDegrees(Long companyId) {
        String sql = """
                SELECT ee.degree AS name, COUNT(DISTINCT ee.user_id) AS headcount
                FROM employee_education ee
                JOIN employee e ON e.user_id = ee.user_id
                WHERE ee.company_id = :companyId
                  AND e.deleted_at IS NULL
                  AND e.resigned_at IS NULL
                  AND e.is_system = 0
                GROUP BY ee.degree
                ORDER BY ee.degree ASC
                """;
        return query(sql, companyId, resultSet -> Degree.valueOf(resultSet.getString("name")).koreanLabel());
    }

    @Override
    public List<NameCount> summarizeCertificates(Long companyId) {
        String sql = """
                SELECT c.name AS name, COUNT(DISTINCT ec.user_id) AS headcount
                FROM employee_certificate ec
                JOIN employee e ON e.user_id = ec.user_id
                JOIN certificate c ON c.certificate_id = ec.certificate_id
                WHERE ec.company_id = :companyId
                  AND e.deleted_at IS NULL
                  AND e.resigned_at IS NULL
                  AND e.is_system = 0
                GROUP BY c.certificate_id, c.name
                ORDER BY c.name ASC
                """;
        return query(sql, companyId, resultSet -> resultSet.getString("name"));
    }

    private interface NameExtractor {
        String extract(java.sql.ResultSet resultSet) throws java.sql.SQLException;
    }

    private List<NameCount> query(String sql, Long companyId, NameExtractor nameExtractor) {
        MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("companyId", companyId);
        return jdbcTemplate.query(sql, parameters, (resultSet, rowNumber) -> new NameCount(
                nameExtractor.extract(resultSet),
                resultSet.getLong("headcount")
        ));
    }
}
