package com.group3.vitamins.department.infrastructure.persistence;

import com.group3.vitamins.department.domain.model.Department;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link DepartmentRepositoryAdapter} 가 <b>같은 상위 부서 안 동명</b>(복합 유니크 {@code uk_department_parent_name})
 * 위반을 <b>커밋이 아니라 쓰기 시점에</b> 터뜨리는지 검증한다.
 *
 * <p>핵심은 <b>수정(rename) 경로</b>다. JPA 는 {@code save}(수정=merge→UPDATE)의 SQL 을 기본적으로 커밋 때 flush
 * 한다. 그러면 UNIQUE 위반이 {@link com.group3.vitamins.department.application.service.DepartmentCommandService#rename}
 * 의 {@code try/catch} <b>밖</b>(커밋 시점)에서 터져 500 이 된다. 어댑터가 {@code saveAndFlush} 로 즉시 실행해야
 * 서비스가 그 위반을 명세의 {@code DEPT_NAME_DUPLICATED}(409)로 변환할 수 있다 (참고: job-position #120·4f7d917).
 *
 * <p>⚠️ 2026-08-06 형제 유니크로 완화됐다. 복합 유니크는 <b>같은 부모 아래 자식</b>만 막으므로 이 테스트는
 * 부모 아래 자식 부서로 검증한다. 최상위(부모 없음)는 MySQL/H2 가 {@code NULL} parent 를 UNIQUE 로 안 막아
 * DB 가 아닌 서비스(app 레벨)가 막는다 → {@link DepartmentCommandServiceTest} 담당.
 *
 * <p>DB 는 H2(MySQL 모드). MySQL 전용 Flyway 는 끄고 스키마는 엔티티에서 만든다({@code ddl-auto=create-drop}) —
 * 보는 건 스키마 정합성이 아니라 <b>제약 위반이 동기적으로 발생하는지</b>다. 그래서 {@code DepartmentJpaEntity}
 * 에 {@code (parent_id, name)} 복합 유니크가 있어야 이 테스트가 성립한다.
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:dept-adapter;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.hikari.connection-init-sql=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(DepartmentRepositoryAdapter.class)
@DisplayName("DepartmentRepositoryAdapter 같은 상위 부서 안 동명 UNIQUE 위반 동기화(flush)")
class DepartmentRepositoryAdapterTest {

    @Autowired
    private DepartmentRepositoryAdapter adapter;

    @Test
    @DisplayName("수정(rename)으로 같은 부모 아래 다른 자식과 이름이 겹치면 저장(saveAndFlush) 시점에 즉시 터진다")
    void duplicateSiblingNameOnRenameThrowsSynchronously() {
        Department parent = adapter.save(Department.create("본부", null));
        adapter.save(Department.create("개발팀", parent.getDepartmentId()));
        Department other = adapter.save(Department.create("영업팀", parent.getDepartmentId()));

        // 같은 부모 아래 "개발팀" 으로 rename → merge→UPDATE. saveAndFlush 라 커밋이 아니라 지금 터져야 한다.
        Department renamed = Department.restore(other.getDepartmentId(), "개발팀", parent.getDepartmentId());

        assertThatThrownBy(() -> adapter.save(renamed))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("생성(create)으로 같은 부모 아래 이름이 중복되면 저장(saveAndFlush) 시점에 즉시 터진다")
    void duplicateSiblingNameOnCreateThrowsSynchronously() {
        Department parent = adapter.save(Department.create("본부", null));
        adapter.save(Department.create("개발팀", parent.getDepartmentId()));

        assertThatThrownBy(() -> adapter.save(Department.create("개발팀", parent.getDepartmentId())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("상위 부서가 다르면 같은 이름을 허용한다 — 기술본부>개발팀 · SI본부>개발팀")
    void sameNameUnderDifferentParentsIsAllowed() {
        Department tech = adapter.save(Department.create("기술본부", null));
        Department si = adapter.save(Department.create("SI본부", null));

        adapter.save(Department.create("개발팀", tech.getDepartmentId()));

        assertThatCode(() -> adapter.save(Department.create("개발팀", si.getDepartmentId())))
                .doesNotThrowAnyException();
    }
}
