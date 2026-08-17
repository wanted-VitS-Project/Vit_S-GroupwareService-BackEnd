package com.group3.vitamins.employee.infrastructure.adapter;

import com.group3.vitamins.employee.application.result.EmployeeSearchRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code search} 가 이름의 {@code %}·{@code _} 를 <b>리터럴로</b> 다루는지 실 SQL 로 검증한다.
 *
 * <p>검색은 로그인 사용자 누구나 호출한다. 이스케이프가 없으면 {@code name='%'} 하나로 부분일치 계약을
 * 우회해 회사 전 사원이 새어 나온다 — {@code #{name}} 바인딩은 주입만 막지 와일드카드는 못 막는다.
 * 정책 단위테스트는 포트를 mock 하므로 이 이스케이프가 SQL 에서 실제로 먹는지는 검증 범위 밖이다.
 */
@MybatisTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:employee-search-wildcard;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.hikari.connection-init-sql=",
        "spring.flyway.enabled=false",
        "mybatis.mapper-locations=classpath:mapper/employee/EmployeeSearchQueryMapper.xml"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan("com.group3.vitamins.employee.infrastructure.adapter")
@Sql(scripts = "/sql/employee-search-wildcard.sql", config = @SqlConfig(encoding = "UTF-8"))
@DisplayName("EmployeeSearchQueryMapper.search — LIKE 와일드카드 이스케이프 (실 SQL)")
class EmployeeSearchQueryMapperWildcardTest {

    private static final long COMPANY = 1L;

    @Autowired
    private EmployeeSearchQueryMapper mapper;

    @Test
    @DisplayName("이름 부분 일치는 그대로 동작한다 — '김' → 김철수·김영희만")
    void partialNameStillMatches() {
        List<EmployeeSearchRow> rows = mapper.search("김", null, COMPANY);

        assertThat(rows).extracting(EmployeeSearchRow::userId)
                .containsExactlyInAnyOrder("vitas-EMP001", "vitas-EMP002");
    }

    @Test
    @DisplayName("name='%' 는 와일드카드가 아니라 리터럴 '%' — 이름에 '%' 든 사원만(전 사원 아님)")
    void percentIsLiteralNotWildcard() {
        List<EmployeeSearchRow> rows = mapper.search("%", null, COMPANY);

        assertThat(rows).extracting(EmployeeSearchRow::userId)
                .containsExactly("vitas-EMP003");   // '100%할인' 만, 5명 전부가 아니다
    }

    @Test
    @DisplayName("name='_' 는 와일드카드가 아니라 리터럴 '_' — 이름에 '_' 든 사원만")
    void underscoreIsLiteralNotWildcard() {
        List<EmployeeSearchRow> rows = mapper.search("_", null, COMPANY);

        assertThat(rows).extracting(EmployeeSearchRow::userId)
                .containsExactly("vitas-EMP004");   // 'a_b테스트' 만
    }

    @Test
    @DisplayName("escape 문자 '!' 입력도 리터럴로 처리 — 아무도 안 맞으면 빈 목록")
    void escapeCharItselfIsLiteral() {
        assertThat(mapper.search("!", null, COMPANY)).isEmpty();
    }
}
