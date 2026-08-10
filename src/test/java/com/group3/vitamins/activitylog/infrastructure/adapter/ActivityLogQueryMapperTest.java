package com.group3.vitamins.activitylog.infrastructure.adapter;

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
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ActivityLogQueryMapper")
class ActivityLogQueryMapperTest {

    private SqlSessionFactory sqlSessionFactory;

    @BeforeEach
    void setUp() throws Exception {
        PooledDataSource dataSource = new PooledDataSource(
                "org.h2.Driver",
                "jdbc:h2:mem:activity-log-query-mapper;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Environment environment = new Environment("test", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.addMapper(ActivityLogQueryMapper.class);

        try (InputStream mapperXml = Resources.getResourceAsStream(
                "mapper/activitylog/ActivityLogQueryMapper.xml")) {
            new org.apache.ibatis.builder.xml.XMLMapperBuilder(
                    mapperXml,
                    configuration,
                    "mapper/activitylog/ActivityLogQueryMapper.xml",
                    configuration.getSqlFragments()
            ).parse();
        }

        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
        resetSchema();
    }

    @Test
    @DisplayName("같은 Step 로그라도 현재 회사의 로그만 조회한다")
    void findActivityLogs_filtersByCompany() throws Exception {
        try (SqlSession session = sqlSessionFactory.openSession();
             Statement statement = session.getConnection().createStatement()) {
            statement.execute("INSERT INTO employee (user_id, name) VALUES ('EMP001', '김용준')");
            statement.execute("INSERT INTO block (block_id, step_id, title, type) VALUES (10, 5, '제안서 작성', 'TEXT')");
            statement.execute("""
                    INSERT INTO activity_log (
                        activity_log_id, company_id, act, resource_id, resource_name,
                        field, before_value, after_value, block_id, user_id, created_at
                    ) VALUES
                        (101, 1, 'modify', 20, '제안서', 'content', '이전', '이후', 10, 'EMP001', CURRENT_TIMESTAMP),
                        (102, 2, 'modify', 20, '제안서', 'content', '타사 이전', '타사 이후', 10, 'EMP001', CURRENT_TIMESTAMP)
                    """);
            session.commit();

            List<ActivityLogRow> rows = session.getMapper(ActivityLogQueryMapper.class)
                    .findActivityLogs(5L, null, null, 21, 1L);

            assertThat(rows).extracting(ActivityLogRow::activityLogId).containsExactly(101L);
        }
    }

    private void resetSchema() throws Exception {
        try (SqlSession session = sqlSessionFactory.openSession();
             Connection connection = session.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS activity_log");
            statement.execute("DROP TABLE IF EXISTS block");
            statement.execute("DROP TABLE IF EXISTS employee");
            statement.execute("""
                    CREATE TABLE employee (
                        user_id VARCHAR(20) PRIMARY KEY,
                        name VARCHAR(100) NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE block (
                        block_id BIGINT PRIMARY KEY,
                        step_id BIGINT NOT NULL,
                        title VARCHAR(200) NOT NULL,
                        type VARCHAR(30) NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE activity_log (
                        activity_log_id BIGINT PRIMARY KEY,
                        company_id BIGINT NOT NULL,
                        act VARCHAR(20) NOT NULL,
                        resource_id BIGINT,
                        resource_name TEXT,
                        field VARCHAR(100),
                        before_value TEXT,
                        after_value TEXT,
                        block_id BIGINT NOT NULL,
                        user_id VARCHAR(20) NOT NULL,
                        created_at DATETIME NOT NULL
                    )
                    """);
        }
    }
}
