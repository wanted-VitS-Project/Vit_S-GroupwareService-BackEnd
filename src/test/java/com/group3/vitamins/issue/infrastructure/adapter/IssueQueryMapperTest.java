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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
            assertThat(issue.version()).isEqualTo(1);
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

    @Test
    @DisplayName("퇴사한 담당자도 이슈 연결을 유지하며 이름과 퇴사일을 함께 조회한다")
    void findAssignees_keepsResignedEmployee() throws Exception {
        LocalDate resignedAt = LocalDate.of(2026, 8, 1);

        try (SqlSession session = sqlSessionFactory.openSession()) {
            insertEmployee(session, "EMP001", "김용준", resignedAt);
            insertAssignee(session, 1L, 101L, "EMP001");
            session.commit();

            List<IssueAssigneeRow> rows = session.getMapper(IssueQueryMapper.class)
                    .findAssignees(List.of(101L));

            assertThat(rows).containsExactly(
                    new IssueAssigneeRow(101L, "EMP001", "김용준", resignedAt));
        }
    }

    @Test
    @DisplayName("담당자 지정 검증용 사원 조회가 퇴사일을 함께 반환한다")
    void findAssigneeCandidates_returnsResignedAt() throws Exception {
        LocalDate resignedAt = LocalDate.of(2026, 8, 1);

        try (SqlSession session = sqlSessionFactory.openSession()) {
            insertEmployee(session, "EMP001", "김용준", resignedAt);
            session.commit();

            List<IssueAssigneeCandidateRow> rows = session.getMapper(IssueQueryMapper.class)
                    .findAssigneeCandidates(List.of("EMP001"));

            assertThat(rows).containsExactly(
                    new IssueAssigneeCandidateRow("EMP001", "김용준", resignedAt));
        }
    }

    @Test
    @DisplayName("본인 담당·미완료·마감일 있는 이슈만 Step·Project 정보와 함께 조회한다")
    void findMyCalendarIssues_returnsOwnUnfinishedIssuesWithProject() throws Exception {
        LocalDateTime dueDate = LocalDateTime.of(2026, 8, 11, 0, 0);

        try (SqlSession session = sqlSessionFactory.openSession()) {
            insertProject(session, 3L, "OO시 스마트도로 구축", null);
            insertStep(session, 10L, 3L, "입찰 진행", null);
            insertIssue(session, 101L, 10L, "제안서 1차 초안 작성", null,
                    "TO_DO", "HIGH", dueDate, null, null);
            insertAssignee(session, 1L, 101L, "EMP001");
            session.commit();

            List<IssueCalendarRow> rows = session.getMapper(IssueQueryMapper.class)
                    .findMyCalendarIssues("EMP001");

            assertThat(rows).hasSize(1);
            IssueCalendarRow row = rows.get(0);
            assertThat(row.issueId()).isEqualTo(101L);
            assertThat(row.version()).isEqualTo(1);
            assertThat(row.title()).isEqualTo("제안서 1차 초안 작성");
            assertThat(row.status()).isEqualTo("TODO");
            assertThat(row.priority()).isEqualTo("HIGH");
            assertThat(row.dueDate()).isEqualTo(dueDate);
            assertThat(row.stepId()).isEqualTo(10L);
            assertThat(row.stepName()).isEqualTo("입찰 진행");
            assertThat(row.projectId()).isEqualTo(3L);
            assertThat(row.projectName()).isEqualTo("OO시 스마트도로 구축");
        }
    }

    @Test
    @DisplayName("DONE 상태 이슈는 캘린더 조회에서 제외한다")
    void findMyCalendarIssues_excludesDoneIssue() throws Exception {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            insertProject(session, 3L, "OO시 스마트도로 구축", null);
            insertStep(session, 10L, 3L, "입찰 진행", null);
            insertIssue(session, 102L, 10L, "완료된 이슈", null,
                    "DONE", "LOW", LocalDateTime.of(2026, 8, 1, 0, 0), null, null);
            insertAssignee(session, 2L, 102L, "EMP001");
            session.commit();

            List<IssueCalendarRow> rows = session.getMapper(IssueQueryMapper.class)
                    .findMyCalendarIssues("EMP001");

            assertThat(rows).isEmpty();
        }
    }

    @Test
    @DisplayName("마감일이 없는 이슈는 캘린더 조회에서 제외한다")
    void findMyCalendarIssues_excludesIssueWithoutDueDate() throws Exception {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            insertProject(session, 3L, "OO시 스마트도로 구축", null);
            insertStep(session, 10L, 3L, "입찰 진행", null);
            insertIssue(session, 103L, 10L, "마감일 없는 이슈", null,
                    "IN_PROGRESS", "MEDIUM", null, null, null);
            insertAssignee(session, 3L, 103L, "EMP001");
            session.commit();

            List<IssueCalendarRow> rows = session.getMapper(IssueQueryMapper.class)
                    .findMyCalendarIssues("EMP001");

            assertThat(rows).isEmpty();
        }
    }

    @Test
    @DisplayName("다른 사용자에게 배정된 이슈는 조회하지 않는다")
    void findMyCalendarIssues_excludesOtherUsersIssue() throws Exception {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            insertProject(session, 3L, "OO시 스마트도로 구축", null);
            insertStep(session, 10L, 3L, "입찰 진행", null);
            insertIssue(session, 104L, 10L, "다른 사람 이슈", null,
                    "IN_PROGRESS", "HIGH", LocalDateTime.of(2026, 8, 12, 0, 0), null, null);
            insertAssignee(session, 4L, 104L, "EMP002");
            session.commit();

            List<IssueCalendarRow> rows = session.getMapper(IssueQueryMapper.class)
                    .findMyCalendarIssues("EMP001");

            assertThat(rows).isEmpty();
        }
    }

    @Test
    @DisplayName("Project가 삭제됐으면 조회하지 않는다")
    void findMyCalendarIssues_excludesDeletedProject() throws Exception {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            insertProject(session, 3L, "삭제된 프로젝트", LocalDateTime.of(2026, 7, 1, 0, 0));
            insertStep(session, 10L, 3L, "삭제된 프로젝트의 스텝", null);
            insertIssue(session, 105L, 10L, "삭제된 프로젝트 이슈", null,
                    "TO_DO", "HIGH", LocalDateTime.of(2026, 8, 13, 0, 0), null, null);
            insertAssignee(session, 5L, 105L, "EMP001");
            session.commit();

            List<IssueCalendarRow> rows = session.getMapper(IssueQueryMapper.class)
                    .findMyCalendarIssues("EMP001");

            assertThat(rows).isEmpty();
        }
    }

    @Test
    @DisplayName("Step이 삭제됐으면 Project가 살아있어도 조회하지 않는다")
    void findMyCalendarIssues_excludesDeletedStep() throws Exception {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            insertProject(session, 4L, "정상 프로젝트", null);
            insertStep(session, 11L, 4L, "삭제된 스텝", LocalDateTime.of(2026, 7, 2, 0, 0));
            insertIssue(session, 106L, 11L, "삭제된 스텝의 이슈", null,
                    "TO_DO", "HIGH", LocalDateTime.of(2026, 8, 14, 0, 0), null, null);
            insertAssignee(session, 6L, 106L, "EMP001");
            session.commit();

            List<IssueCalendarRow> rows = session.getMapper(IssueQueryMapper.class)
                    .findMyCalendarIssues("EMP001");

            assertThat(rows).isEmpty();
        }
    }

    private void resetSchema() throws Exception {
        try (SqlSession session = sqlSessionFactory.openSession();
             Connection connection = session.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS issue_assign");
            statement.execute("DROP TABLE IF EXISTS employee");
            statement.execute("DROP TABLE IF EXISTS issue");
            statement.execute("DROP TABLE IF EXISTS step");
            statement.execute("DROP TABLE IF EXISTS project");
            statement.execute("""
                    CREATE TABLE issue (
                        issue_id BIGINT PRIMARY KEY,
                        step_id BIGINT NOT NULL,
                        title VARCHAR(200),
                        content TEXT,
                        status VARCHAR(20) NOT NULL,
                        priority VARCHAR(20) NOT NULL,
                        version INT NOT NULL DEFAULT 1,
                        due_date DATETIME NULL,
                        finish_day DATETIME NULL,
                        deleted_at DATETIME NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE issue_assign (
                        issue_assign_id BIGINT PRIMARY KEY,
                        issue_id BIGINT NOT NULL,
                        user_id VARCHAR(20) NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE employee (
                        user_id VARCHAR(20) PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        resigned_at DATE NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE step (
                        step_id BIGINT PRIMARY KEY,
                        project_id BIGINT NOT NULL,
                        name VARCHAR(200) NOT NULL,
                        deleted_at DATETIME NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE project (
                        project_id BIGINT PRIMARY KEY,
                        name VARCHAR(300) NOT NULL,
                        deleted_at DATETIME NULL
                    )
                    """);
        }
    }

    private void insertAssignee(SqlSession session, Long issueAssignId, Long issueId, String userId)
            throws Exception {
        try (PreparedStatement statement = session.getConnection().prepareStatement("""
                INSERT INTO issue_assign (issue_assign_id, issue_id, user_id)
                VALUES (?, ?, ?)
                """)) {
            statement.setLong(1, issueAssignId);
            statement.setLong(2, issueId);
            statement.setString(3, userId);
            statement.executeUpdate();
        }
    }

    private void insertEmployee(SqlSession session, String userId, String name, LocalDate resignedAt)
            throws Exception {
        try (PreparedStatement statement = session.getConnection().prepareStatement("""
                INSERT INTO employee (user_id, name, resigned_at)
                VALUES (?, ?, ?)
                """)) {
            statement.setString(1, userId);
            statement.setString(2, name);
            statement.setObject(3, resignedAt);
            statement.executeUpdate();
        }
    }

    private void insertStep(SqlSession session, Long stepId, Long projectId, String name, LocalDateTime deletedAt)
            throws Exception {
        try (PreparedStatement statement = session.getConnection().prepareStatement("""
                INSERT INTO step (step_id, project_id, name, deleted_at)
                VALUES (?, ?, ?, ?)
                """)) {
            statement.setLong(1, stepId);
            statement.setLong(2, projectId);
            statement.setString(3, name);
            statement.setTimestamp(4, toTimestamp(deletedAt));
            statement.executeUpdate();
        }
    }

    private void insertProject(SqlSession session, Long projectId, String name, LocalDateTime deletedAt)
            throws Exception {
        try (PreparedStatement statement = session.getConnection().prepareStatement("""
                INSERT INTO project (project_id, name, deleted_at)
                VALUES (?, ?, ?)
                """)) {
            statement.setLong(1, projectId);
            statement.setString(2, name);
            statement.setTimestamp(3, toTimestamp(deletedAt));
            statement.executeUpdate();
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
