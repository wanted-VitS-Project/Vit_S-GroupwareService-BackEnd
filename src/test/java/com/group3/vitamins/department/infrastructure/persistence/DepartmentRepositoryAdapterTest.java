package com.group3.vitamins.department.infrastructure.persistence;

import com.group3.vitamins.department.domain.model.Department;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link DepartmentRepositoryAdapter} 가 <b>같은 회사·상위 부서 안 동명</b>(복합 유니크
 * {@code uk_department_company_parent_name(company_id, parent_key, name)}) 위반을 <b>커밋이 아니라 쓰기 시점에</b>
 * 터뜨리는지, 그리고 조회가 <b>회사 범위로 격리</b>되는지 검증한다.
 *
 * <p>핵심은 <b>수정(rename) 경로</b>다. JPA 는 {@code save}(수정=merge→UPDATE)의 SQL 을 기본적으로 커밋 때 flush
 * 한다. 그러면 UNIQUE 위반이 {@link com.group3.vitamins.department.application.service.DepartmentCommandService#rename}
 * 의 {@code try/catch} <b>밖</b>(커밋 시점)에서 터져 500 이 된다. 어댑터가 {@code saveAndFlush} 로 즉시 실행해야
 * 서비스가 그 위반을 명세의 {@code DEPT_NAME_DUPLICATED}(409)로 변환할 수 있다 (참고: job-position #120·4f7d917).
 *
 * <p>⚠️ 2026-08-06 형제 유니크로 완화됐다. 복합 유니크는 생성 열 {@code parent_key = COALESCE(parent_id, 0)}
 * 에 걸려, <b>자식(같은 부모)뿐 아니라 최상위(부모 없음 → 0 공유) 동명까지 DB 가 막는다.</b> 그래서 이 테스트는
 * 자식 케이스와 최상위 케이스를 모두 검증한다. 상위가 다르면 같은 이름은 허용돼야 한다.
 *
 * <p>DB 는 H2(MySQL 모드). MySQL 전용 Flyway 는 끄고 스키마는 엔티티에서 만든다({@code ddl-auto=create-drop}) —
 * 보는 건 스키마 정합성이 아니라 <b>제약 위반이 동기적으로 발생하는지</b>다. 그래서 {@code DepartmentJpaEntity}
 * 의 {@code parent_key} 생성 열 + {@code (company_id, parent_key, name)} 복합 유니크가 있어야 이 테스트가 성립한다.
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
        Department parent = adapter.save(Department.create("본부", null, 1L));
        adapter.save(Department.create("개발팀", parent.getDepartmentId(), 1L));
        Department other = adapter.save(Department.create("영업팀", parent.getDepartmentId(), 1L));

        // 같은 부모 아래 "개발팀" 으로 rename → merge→UPDATE. saveAndFlush 라 커밋이 아니라 지금 터져야 한다.
        Department renamed = Department.restore(other.getDepartmentId(), 1L, "개발팀", parent.getDepartmentId());

        assertThatThrownBy(() -> adapter.save(renamed))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("생성(create)으로 같은 부모 아래 이름이 중복되면 저장(saveAndFlush) 시점에 즉시 터진다")
    void duplicateSiblingNameOnCreateThrowsSynchronously() {
        Department parent = adapter.save(Department.create("본부", null, 1L));
        adapter.save(Department.create("개발팀", parent.getDepartmentId(), 1L));

        assertThatThrownBy(() -> adapter.save(Department.create("개발팀", parent.getDepartmentId(), 1L)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("최상위(부모 없음)끼리 이름이 겹치면 저장(saveAndFlush) 시점에 즉시 터진다 — parent_key=0 공유")
    void duplicateRootNameOnCreateThrowsSynchronously() {
        adapter.save(Department.create("본부", null, 1L));

        assertThatThrownBy(() -> adapter.save(Department.create("본부", null, 1L)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("수정(rename)으로 최상위 다른 부서와 이름이 겹치면 저장(saveAndFlush) 시점에 즉시 터진다")
    void duplicateRootNameOnRenameThrowsSynchronously() {
        adapter.save(Department.create("경영지원본부", null, 1L));
        Department other = adapter.save(Department.create("기술본부", null, 1L));

        Department renamed = Department.restore(other.getDepartmentId(), 1L, "경영지원본부", null);

        assertThatThrownBy(() -> adapter.save(renamed))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("상위 부서가 다르면 같은 이름을 허용한다 — 기술본부>개발팀 · SI본부>개발팀")
    void sameNameUnderDifferentParentsIsAllowed() {
        Department tech = adapter.save(Department.create("기술본부", null, 1L));
        Department si = adapter.save(Department.create("SI본부", null, 1L));

        adapter.save(Department.create("개발팀", tech.getDepartmentId(), 1L));

        assertThatCode(() -> adapter.save(Department.create("개발팀", si.getDepartmentId(), 1L)))
                .doesNotThrowAnyException();
    }

    // ===== 회사 범위 격리 (P1-1b) =====

    @Test
    @DisplayName("회사가 다르면 같은 최상위 부서명을 허용한다 — company_id 가 유니크 키에 포함")
    void sameTopLevelNameAcrossDifferentCompaniesIsAllowed() {
        adapter.save(Department.create("본사", null, 1L));

        // 다른 회사(2L)의 같은 최상위 이름은 uk_department_company_parent_name 이 회사로 구분해 허용해야 한다.
        assertThatCode(() -> adapter.save(Department.create("본사", null, 2L)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("findById 는 회사 범위 — 타사 부서 ID 는 조회되지 않는다(→ 서비스에서 404)")
    void findByIdExcludesOtherCompany() {
        Department saved = adapter.save(Department.create("본사", null, 1L));

        assertThat(adapter.findById(saved.getDepartmentId(), 1L)).isPresent();
        assertThat(adapter.findById(saved.getDepartmentId(), 2L)).isEmpty();
    }

    @Test
    @DisplayName("형제명 중복 검사는 회사 범위 — 최상위(parentId=null)도 회사별로 따로 센다")
    void existsSiblingNameIsScopedByCompany() {
        adapter.save(Department.create("영업본부", null, 1L));

        assertThat(adapter.existsSiblingName("영업본부", null, 1L)).isTrue();
        assertThat(adapter.existsSiblingName("영업본부", null, 2L)).isFalse();
    }

    @Test
    @DisplayName("하위 부서 수는 회사 범위로 센다 — 같은 parentId 값이라도 타사 자식은 세지 않는다")
    void countByParentIdIsScopedByCompany() {
        Department parent = adapter.save(Department.create("본부", null, 1L));
        adapter.save(Department.create("개발팀", parent.getDepartmentId(), 1L));

        assertThat(adapter.countByParentId(parent.getDepartmentId(), 1L)).isEqualTo(1L);
        assertThat(adapter.countByParentId(parent.getDepartmentId(), 2L)).isZero();
    }
}
