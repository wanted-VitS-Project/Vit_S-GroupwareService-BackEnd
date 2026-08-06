package com.group3.vitamins.issue.infrastructure.adapter;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IssueQueryMapper")
class IssueQueryMapperTest {

    private SqlSessionFactory sqlSessionFactory;

    @BeforeEach
    void setUp() throws Exception {
        PooledDataSource dataSource = new PooledDataSource(
                "org.h2.Driver",
                "jdbc:h2:mem:issue-query-mapper;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Environment environment = new Environment(
                "test",
                new JdbcTransactionFactory(),
                dataSource
        );
        Configuration configuration = new Configuration(environment);
        configuration.addMapper(IssueQueryMapper.class);

        try (InputStream mapperXml = Resources.getResourceAsStream("mapper/issue/IssueQueryMapper.xml")) {
            new org.apache.ibatis.builder.xml.XMLMapperBuilder(
                    mapperXml,
                    configuration,
                    "mapper/issue/IssueQueryMapper.xml",
                    configuration.getSqlFragments()
            ).parse();
        }

        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
        resetSchema();
    }

    @Test
    @DisplayName("활성 이슈를 전체 필드와 함께 조회하고 DB TO_DO 상태를 API TODO 값으로 변환한다")
    void findIssue_activeIssue() throws Exception {
        LocalDateTime dueDate = LocalDateTime.of(2026, 8, 5, 18, 0);
        LocalDateTime completedAt = LocalDateTime.of(2026, 8, 4, 16, 30);

        try (SqlSession session = sqlSessionFactory.openSession()) {
            insertIssue(session, 101L, 10L, "제안서 1차 초안 작성", "공고 요구사항에 맞춰 작성",
                    "TO_DO", "HIGH", dueDate, completedAt, null);
            session.commit();

            Optional<IssueRow> result = session.getMapper(IssueQueryMapper.class).findIssue(101L);

            assertThat(result).isPresent();
            IssueRow issue = result.orElseThrow();
            assertThat(issue.issueId()).isEqualTo(101L);
            assertThat(issue.stepId()).isEqualTo(10L);
            assertThat(issue.title()).isEqualTo("제안서 1차 초안 작성");
            assertThat(issue.content()).isEqualTo("공고 요구사항에 맞춰 작성");
            assertThat(issue.status()).isEqualTo("TODO");
            assertThat(issue.priority()).isEqualTo("HIGH");
            assertThat(issue.dueDate()).isEqualTo(dueDate);
            assertThat(issue.completedAt()).isEqualTo(completedAt);
        }
    }

    @Test
    @DisplayName("논리 삭제된 이슈는 조회하지 않는다")
    void findIssue_deletedIssue() throws Exception {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            insertIssue(session, 102L, 10L, "삭제된 이슈", null,
                    "IN_PROGRESS", "MEDIUM", null, null,
                    LocalDateTime.of(2026, 8, 6, 12, 0));
            session.commit();

            Optional<IssueRow> result = session.getMapper(IssueQueryMapper.class).findIssue(102L);

            assertThat(result).isEmpty();
        }
    }

    private void resetSchema() throws Exception {
        try (SqlSession session = sqlSessionFactory.openSession();
             Connection connection = session.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS issue");
            statement.execute("""
                    CREATE TABLE issue (
                        issue_id BIGINT PRIMARY KEY,
                        step_id BIGINT NOT NULL,
                        title VARCHAR(200),
                        content TEXT,
                        status VARCHAR(20) NOT NULL,
                        priority VARCHAR(20) NOT NULL,
                        due_date DATETIME NULL,
                        finish_day DATETIME NULL,
                        deleted_at DATETIME NULL
                    )
                    """);
        }
    }

    private void insertIssue(
            SqlSession session,
            Long issueId,
            Long stepId,
            String title,
            String content,
            String status,
            String priority,
            LocalDateTime dueDate,
            LocalDateTime completedAt,
            LocalDateTime deletedAt
    ) throws Exception {
        try (PreparedStatement statement = session.getConnection().prepareStatement("""
                INSERT INTO issue (
                    issue_id, step_id, title, content, status, priority,
                    due_date, finish_day, deleted_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setLong(1, issueId);
            statement.setLong(2, stepId);
            statement.setString(3, title);
            statement.setString(4, content);
            statement.setString(5, status);
            statement.setString(6, priority);
            statement.setTimestamp(7, toTimestamp(dueDate));
            statement.setTimestamp(8, toTimestamp(completedAt));
            statement.setTimestamp(9, toTimestamp(deletedAt));
            statement.executeUpdate();
        }
    }

    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }
}
