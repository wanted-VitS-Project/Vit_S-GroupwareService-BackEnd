package com.group3.vitamins.approval.infrastructure.persistence.mapper;

import com.group3.vitamins.approval.infrastructure.persistence.row.ApprovalEmployeeRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code findEmployee} 가 <b>직급명</b>을 실제로 실어 오는지 실 SQL 로 검증한다.
 *
 * <p>결재선 자격 판정은 대표를 전역 role 이 아니라 직급명({@code job_position.name})으로 가린다
 * ({@code ApprovalLineEligibilityPolicy}). 그런데 정책 단위테스트는 포트를 mock 하므로
 * <b>조인이 그 값을 실제로 채우는지</b>는 검증 범위 밖이다 — 조인이나 별칭이 어긋나도 컴파일은 통과하고,
 * 그때 증상은 "대표인데 결재자로 지정 안 됨"으로만 나타난다.
 */
@MybatisTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:approval-employee-position;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.hikari.connection-init-sql=",
        "spring.flyway.enabled=false",
        "mybatis.mapper-locations=classpath:mapper/approval/ApprovalQueryMapper.xml"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan("com.group3.vitamins.approval.infrastructure.persistence.mapper")
// encoding 미지정 시 윈도우에서 픽스처 한글이 MS949 로 읽혀 깨진다 — 직급명 비교가 목적이라 치명적이다.
@Sql(scripts = "/sql/approval-employee-position.sql", config = @SqlConfig(encoding = "UTF-8"))
@DisplayName("ApprovalQueryMapper.findEmployee — 직급명 매핑 (실 SQL)")
class ApprovalQueryMapperEmployeePositionTest {

    private static final String REPRESENTATIVE = "vitas-EMP100";
    private static final String NO_POSITION = "vitas-EMP101";

    @Autowired
    private ApprovalQueryMapper approvalQueryMapper;

    @Test
    @DisplayName("직급이 대표인 사원은 jobPositionName 에 '대표'가 그대로 실려 온다")
    void representativeJobPositionNameIsMapped() {
        Optional<ApprovalEmployeeRow> row = approvalQueryMapper.findEmployee(REPRESENTATIVE);

        assertThat(row).isPresent();
        // 정책이 이 문자열과 정확히 비교한다 — 공백·표기가 달라지면 면제가 조용히 사라진다.
        assertThat(row.get().jobPositionName()).isEqualTo("대표");
        assertThat(row.get().role()).isEqualTo("MEMBER");
        assertThat(row.get().accountStatus()).isEqualTo("ACTIVE");
        assertThat(row.get().companyId()).isEqualTo(1L);
        assertThat(row.get().departmentPath()).isEqualTo("본사 / 개발팀");
    }

    @Test
    @DisplayName("직급 미지정 사원은 jobPositionName 이 null 이다 — 대표 면제가 걸리지 않는다")
    void missingJobPositionIsNull() {
        Optional<ApprovalEmployeeRow> row = approvalQueryMapper.findEmployee(NO_POSITION);

        assertThat(row).isPresent();
        assertThat(row.get().jobPositionName()).isNull();
    }
}
