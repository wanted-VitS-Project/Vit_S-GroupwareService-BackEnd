package com.group3.vitamins.employee.infrastructure.persistence;

import com.group3.vitamins.employee.domain.model.Employee;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link EmployeeRepositoryAdapter} 의 저장 의미를 검증한다.
 *
 * <p>핵심 — 사번은 <b>할당 String PK</b>라 {@code Persistable} 로 {@code persist(INSERT)} 를 강제하지 않으면
 * 중복 저장이 {@code merge}(UPDATE)가 되어 기존 행을 조용히 덮어쓴다. 이 테스트는 저장이 진짜 INSERT 이고,
 * 중복 PK 가 {@code saveAndFlush} 시점에 즉시 {@code DataIntegrityViolationException} 으로 터지는지 본다
 * (등록 유스케이스가 이를 409 로 변환한다).
 *
 * <p>DB 는 H2(MySQL 모드). MySQL 전용 Flyway 는 끄고 스키마는 엔티티에서 만든다.
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:employee-adapter;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.hikari.connection-init-sql=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(EmployeeRepositoryAdapter.class)
@DisplayName("EmployeeRepositoryAdapter 저장 의미(INSERT·중복 방어)")
class EmployeeRepositoryAdapterTest {

    @Autowired
    private EmployeeRepositoryAdapter adapter;

    private Employee sample(String userId) {
        return Employee.register(userId, "홍길동", 2L, 10L, "hong@vitamins.com", "010-1111-2222",
                LocalDate.of(2026, 8, 5));
    }

    @Test
    @DisplayName("신규 사원은 저장되고 existsById 로 확인된다")
    void savesNewEmployee() {
        adapter.save(sample("EMP021"));

        assertThat(adapter.existsById("EMP021")).isTrue();
        assertThat(adapter.existsById("EMP999")).isFalse();
    }

    @Test
    @DisplayName("같은 사번을 다시 저장하면 merge(UPDATE)가 아니라 PK 중복으로 즉시 터진다")
    void duplicateUserIdThrowsSynchronously() {
        adapter.save(sample("EMP021"));

        assertThatThrownBy(() -> adapter.save(sample("EMP021")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("updateInfo 는 로드한 엔티티(isNew=false)를 UPDATE 한다 — PK 충돌 없이 필드가 바뀐다")
    void updateInfoModifiesExisting() {
        adapter.save(sample("EMP021"));

        Employee current = adapter.findById("EMP021").orElseThrow();
        adapter.updateInfo(current.withInfo("김철수", "010-9999-8888", null, 2L, null,
                LocalDate.of(2024, 3, 2)));

        Employee reloaded = adapter.findById("EMP021").orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("김철수");
        assertThat(reloaded.getEmail()).isNull();          // 명시적으로 지움
        assertThat(reloaded.getJobPositionId()).isNull();  // 직급 미지정으로 변경
    }

    @Test
    @DisplayName("resign 은 퇴사일만 기록한다")
    void resignRecordsDate() {
        adapter.save(sample("EMP021"));

        adapter.resign("EMP021", LocalDate.of(2026, 8, 31));

        assertThat(adapter.findById("EMP021").orElseThrow().getResignedAt())
                .isEqualTo(LocalDate.of(2026, 8, 31));
    }

    @Test
    @DisplayName("updateInfo 는 퇴사일을 덮어쓰지 않는다 — 퇴사 후 정보 수정해도 resigned_at 유지")
    void updateInfoDoesNotClobberResignation() {
        adapter.save(sample("EMP021"));
        adapter.resign("EMP021", LocalDate.of(2026, 8, 31));

        // 퇴사 이전 스냅샷(resignedAt=null)으로 정보 수정 — 동시 수정 시나리오. resigned_at 을 되돌리면 안 된다.
        Employee stale = Employee.restore("EMP021", "홍길동", false, 2L, 10L,
                "hong@vitamins.com", "010-1111-2222", LocalDate.of(2024, 3, 2), null);
        adapter.updateInfo(stale.withInfo("김철수", "010-9999-8888", "hong@vitamins.com", 2L, 10L,
                LocalDate.of(2024, 3, 2)));

        Employee reloaded = adapter.findById("EMP021").orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("김철수");                       // 정보는 반영
        assertThat(reloaded.getResignedAt()).isEqualTo(LocalDate.of(2026, 8, 31)); // 퇴사일 유지
    }
}
