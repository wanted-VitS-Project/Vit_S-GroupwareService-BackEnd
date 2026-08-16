package com.group3.vitamins.project.step.infrastructure.adapter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 4회 조회를 합친 판정 쿼리를 <b>실제로 돌려서</b> 검증한다.
 *
 * <p>서비스 단위테스트({@code StepAccessServiceTest})는 포트를 목으로 막아서, 이 SQL 이
 * 무엇을 돌려주는지는 전혀 보지 않는다. 그런데 이 쿼리의 위험은 전부 SQL 안에 있다 —
 * 회사·참여자 조건을 {@code ON} 이 아니라 {@code WHERE} 에 두면 <b>403 이어야 할 것이 404 로 나가고,
 * 컴파일도 테스트도(단위테스트만 보면) 통과한다.</b> 그래서 실 SQL 검증이 필요하다.
 */
@MybatisTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:step-access-query;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.hikari.connection-init-sql=",
        "spring.flyway.enabled=false",
        "mybatis.mapper-locations=classpath:mapper/step/StepAccessQueryMapper.xml"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan("com.group3.vitamins.project.step.infrastructure.adapter")
// ⚠️ encoding 을 명시하지 않으면 스크립트를 플랫폼 기본 charset(윈도우는 MS949)으로 읽어
//    픽스처의 한글이 깨진 채 적재된다.
@Sql(scripts = "/sql/step-access-query.sql", config = @SqlConfig(encoding = "UTF-8"))
@DisplayName("StepAccessQueryMapper — 판정 재료 조회 (실 SQL)")
class StepAccessQueryMapperTest {

    private static final Long COMPANY_1 = 1L;
    private static final String MEMBER = "vitas-1000001";
    private static final String STRANGER = "vitas-9999999";

    @Autowired
    private StepAccessQueryMapper mapper;

    @Test
    @DisplayName("참여자 권한과 오버라이드를 한 행으로 가져온다 — 오버라이드가 없으면 null")
    void 참여자_권한_상속() {
        StepAccessRow row = mapper.findAccess(10L, MEMBER, COMPANY_1);

        assertThat(row).isNotNull();
        assertThat(row.stepId()).isEqualTo(10L);
        assertThat(row.projectId()).isEqualTo(1L);
        assertThat(row.projectVisible()).isTrue();
        assertThat(row.memberPermission()).isEqualTo("EDITOR");
        assertThat(row.overridePermission()).isNull();
    }

    @Test
    @DisplayName("오버라이드 NONE 은 null 이 아니라 'NONE' 문자열로 와야 한다")
    void 오버라이드_NONE_은_null_이_아니다() {
        StepAccessRow row = mapper.findAccess(11L, MEMBER, COMPANY_1);

        assertThat(row).isNotNull();
        assertThat(row.memberPermission()).isEqualTo("EDITOR");
        assertThat(row.overridePermission()).isEqualTo("NONE");
    }

    @Test
    @DisplayName("⭐ 다른 회사 프로젝트의 스텝은 행이 사라지지 않고 projectVisible=false 로 온다 (404 아님)")
    void 타회사_는_행이_남는다() {
        StepAccessRow row = mapper.findAccess(20L, MEMBER, COMPANY_1);

        assertThat(row).isNotNull();
        assertThat(row.stepId()).isEqualTo(20L);
        assertThat(row.projectVisible()).isFalse();
    }

    @Test
    @DisplayName("⭐ 삭제된 프로젝트의 스텝도 행은 남고 projectVisible=false 다")
    void 삭제된_프로젝트_는_행이_남는다() {
        StepAccessRow row = mapper.findAccess(40L, MEMBER, COMPANY_1);

        assertThat(row).isNotNull();
        assertThat(row.projectVisible()).isFalse();
    }

    @Test
    @DisplayName("⭐ 참여자가 아니어도 행은 남고 memberPermission 만 null 이다 (404 아님)")
    void 미참여_는_행이_남는다() {
        StepAccessRow row = mapper.findAccess(10L, STRANGER, COMPANY_1);

        assertThat(row).isNotNull();
        assertThat(row.projectVisible()).isTrue();
        assertThat(row.memberPermission()).isNull();
        assertThat(row.overridePermission()).isNull();
    }

    @Test
    @DisplayName("스텝이 논리 삭제됐으면 null — 이것만이 404 다")
    void 삭제된_스텝_은_null() {
        assertThat(mapper.findAccess(30L, MEMBER, COMPANY_1)).isNull();
    }

    @Test
    @DisplayName("없는 스텝이면 null")
    void 없는_스텝_은_null() {
        assertThat(mapper.findAccess(999L, MEMBER, COMPANY_1)).isNull();
    }
}
