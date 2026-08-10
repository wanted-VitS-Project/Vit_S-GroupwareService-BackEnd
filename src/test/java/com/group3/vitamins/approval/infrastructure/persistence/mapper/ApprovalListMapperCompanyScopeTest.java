package com.group3.vitamins.approval.infrastructure.persistence.mapper;

import com.group3.vitamins.approval.infrastructure.persistence.row.ApprovalListRow;
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
 * 결재관리 목록조회의 <b>회사(테넌트) 격리</b>를 실제 SQL 실행으로 검증한다.
 *
 * <p>서비스 단위테스트({@code ApprovalQueryServiceListScopeTest})는 "매퍼에 {@code companyId} 를
 * 넘겼는가"까지만 본다. 그 값이 XML 의 {@code #{companyId}} 에 실제로 바인딩되는지, 회사가 다른 행이
 * 정말 빠지는지는 <b>쿼리를 돌려봐야만</b> 알 수 있다 — 파라미터명이 어긋나도 컴파일은 통과한다.
 *
 * <p>픽스처는 회사 2개를 만들고 <b>base 사번을 일부러 겹치게</b> 두었다(양쪽 다 {@code 1234567}).
 * 접두사 방식({@code {company_code}-{사번}})이 아니었다면 충돌했을 구성이라, 회사 격리와 전역 유일
 * 접두사를 한 번에 확인한다.
 */
@MybatisTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:approval-list-company-scope;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.hikari.connection-init-sql=",
        "spring.flyway.enabled=false",
        "mybatis.mapper-locations=classpath:mapper/approval/ApprovalListMapper.xml"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan("com.group3.vitamins.approval.infrastructure.persistence.mapper")
// ⚠️ encoding 을 명시하지 않으면 스크립트를 플랫폼 기본 charset(윈도우는 MS949)으로 읽어
//    픽스처의 한글이 깨진 채 적재된다. 개발자 OS 에 따라 결과가 갈리므로 못 박는다.
@Sql(scripts = "/sql/approval-list-company-scope.sql", config = @SqlConfig(encoding = "UTF-8"))
@DisplayName("ApprovalListMapper — 회사 격리 (실 SQL)")
class ApprovalListMapperCompanyScopeTest {

    private static final Long COMPANY_1 = 1L;
    private static final Long COMPANY_2 = 2L;
    private static final Long ABSENT_COMPANY = 999L;

    @Autowired
    private ApprovalListMapper mapper;

    @Test
    @DisplayName("회사 1로 조회하면 회사 1의 결재만 나온다 — 회사 2 것은 빠진다")
    void findsOnlyOwnCompanyRows() {
        List<ApprovalListRow> rows = findAll(COMPANY_1);

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.approvalId()).isEqualTo(100L);
            assertThat(row.title()).isEqualTo("회사1 품의서");
            assertThat(row.drafterId()).isEqualTo("vitas-1234567");
            assertThat(row.currentApproverId()).isEqualTo("vitas-7654321");
        });
    }

    @Test
    @DisplayName("회사 2로 조회하면 회사 2의 결재만 나온다 — base 사번이 같아도 섞이지 않는다")
    void findsOnlyOtherCompanyRowsForOtherCompany() {
        List<ApprovalListRow> rows = findAll(COMPANY_2);

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.approvalId()).isEqualTo(101L);
            assertThat(row.drafterId()).isEqualTo("acme-1234567");
        });
    }

    @Test
    @DisplayName("countApprovals 도 같은 회사 조건을 적용한다 — 목록과 페이징 총계가 어긋나지 않는다")
    void countIsScopedToCompany() {
        assertThat(count(COMPANY_1)).isEqualTo(1);
        assertThat(count(COMPANY_2)).isEqualTo(1);
    }

    @Test
    @DisplayName("존재하지 않는 회사로 조회하면 빈 결과다 — 전 회사가 새는 일은 없다")
    void unknownCompanySeesNothing() {
        assertThat(findAll(ABSENT_COMPANY)).isEmpty();
        assertThat(count(ABSENT_COMPANY)).isZero();
    }

    /** 필터를 전부 {@code null} 로 둔 {@code scope=all} 상황 — 회사 조건만 남는 가장 위험한 경로다. */
    private List<ApprovalListRow> findAll(Long companyId) {
        return mapper.findApprovals(companyId, null, null, null, null, null, null, null, null, 0, 10);
    }

    private long count(Long companyId) {
        return mapper.countApprovals(companyId, null, null, null, null, null, null, null, null);
    }
}
