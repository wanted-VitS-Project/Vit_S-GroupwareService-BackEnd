package com.group3.vitamins.employee.infrastructure.adapter;

import com.group3.vitamins.employee.application.query.EmployeeListCriteria;
import com.group3.vitamins.employee.application.result.EmployeeListRow;
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
 * {@code findPage/count} 의 {@code includeSubDepartments} 필터가 실제 SQL 에서 하위 부서까지 덮는지 검증한다 (employee.md §1).
 *
 * <p>정책 단위테스트는 포트를 mock 하므로 "OR parent_id 서브쿼리"가 실제로 하위 사원을 포함/타 최상위를 배제하는지는
 * 여기서만 드러난다. 트리는 최대 2단이라 자기 자신 OR 자식으로 전 소속을 덮는다.
 */
@MybatisTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:employee-subdept;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.hikari.connection-init-sql=",
        "spring.flyway.enabled=false",
        "mybatis.mapper-locations=classpath:mapper/employee/EmployeeAdminQueryMapper.xml"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan("com.group3.vitamins.employee.infrastructure.adapter")
@Sql(scripts = "/sql/employee-subdepartment-filter.sql", config = @SqlConfig(encoding = "UTF-8"))
@DisplayName("EmployeeAdminQueryMapper.findPage — includeSubDepartments 하위 부서 필터 (실 SQL)")
class EmployeeAdminQuerySubDepartmentTest {

    private static final long COMPANY = 1L;

    @Autowired
    private EmployeeAdminQueryMapper mapper;

    /** keyword·role·status 필터 없이 부서만 거는 조건. resignedOnly=false → 재직자만. */
    private EmployeeListCriteria byDepartment(Long departmentId, boolean includeSub) {
        return new EmployeeListCriteria(null, departmentId, includeSub, null, null, null,
                false, 0, 50, COMPANY);
    }

    @Test
    @DisplayName("false 면 직속만 — 기술본부(1)는 본부장 1명")
    void directOnly() {
        EmployeeListCriteria c = byDepartment(1L, false);

        assertThat(mapper.count(c)).isEqualTo(1);
        assertThat(mapper.findPage(c)).extracting(EmployeeListRow::userId)
                .containsExactly("vitas-EMP001");
    }

    @Test
    @DisplayName("true 면 하위 포함 — 기술본부(1)는 직속+개발팀+인사팀 4명, 별도 최상위(재무팀)는 제외")
    void includesSubDepartments() {
        EmployeeListCriteria c = byDepartment(1L, true);

        assertThat(mapper.count(c)).isEqualTo(4);
        assertThat(mapper.findPage(c)).extracting(EmployeeListRow::userId)
                .containsExactlyInAnyOrder("vitas-EMP001", "vitas-EMP002", "vitas-EMP003", "vitas-EMP004")
                .doesNotContain("vitas-EMP005"); // 재무팀은 기술본부 하위가 아니다
    }

    @Test
    @DisplayName("하위 부서(개발팀 2)에 true 를 걸면 자식이 없어 직속과 동일 — 2명")
    void leafDepartmentUnaffected() {
        EmployeeListCriteria c = byDepartment(2L, true);

        assertThat(mapper.count(c)).isEqualTo(2);
        assertThat(mapper.findPage(c)).extracting(EmployeeListRow::userId)
                .containsExactlyInAnyOrder("vitas-EMP002", "vitas-EMP003");
    }

    @Test
    @DisplayName("count 와 findPage 는 같은 필터를 본다 — 하위 포함 시 둘 다 4")
    void countMatchesPage() {
        EmployeeListCriteria c = byDepartment(1L, true);

        assertThat(mapper.count(c)).isEqualTo(mapper.findPage(c).size());
    }
}
