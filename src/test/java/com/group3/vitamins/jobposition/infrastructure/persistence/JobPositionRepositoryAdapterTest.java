package com.group3.vitamins.jobposition.infrastructure.persistence;

import com.group3.vitamins.jobposition.domain.model.JobPosition;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link JobPositionRepositoryAdapter} 가 제약 위반을 <b>커밋이 아니라 쓰기 시점에</b> 터뜨리는지 검증한다.
 *
 * <p>JPA 는 {@code save}(수정)·{@code deleteById} 의 SQL 을 기본적으로 커밋 때 flush 한다. 그러면 제약 위반이
 * 서비스의 {@code try/catch} <b>밖</b>(커밋 시점)에서 터져 500 이 된다. 어댑터가 {@code saveAndFlush}/{@code flush}
 * 로 즉시 실행해야 서비스가 {@code POS_NAME_DUPLICATED}/{@code POS_IN_USE}(409)로 변환할 수 있다.
 *
 * <p>DB 는 H2(MySQL 모드). MySQL 전용 Flyway 는 끄고 스키마는 엔티티에서 만든다 —
 * 보는 건 스키마 정합성이 아니라 <b>제약 위반이 동기적으로 발생하는지</b>다.
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:jobpos-adapter;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.hikari.connection-init-sql=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JobPositionRepositoryAdapter.class)
@DisplayName("JobPositionRepositoryAdapter 제약 위반 동기화(flush)")
class JobPositionRepositoryAdapterTest {

    @Autowired
    private JobPositionRepositoryAdapter adapter;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("이름이 중복되면 저장(saveAndFlush) 시점에 DataIntegrityViolationException 이 즉시 터진다")
    void duplicateNameThrowsSynchronouslyOnSave() {
        adapter.save(JobPosition.create("사원", 1));

        assertThatThrownBy(() -> adapter.save(JobPosition.create("사원", 2)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("참조하는 사원이 있으면 삭제(delete+flush) 시점에 FK 위반이 즉시 터진다")
    void foreignKeyViolationThrowsSynchronouslyOnDelete() {
        JobPosition saved = adapter.save(JobPosition.create("대리", 1));
        Long id = saved.getJobPositionId();

        // employee 에는 이제 JPA 엔티티(EmployeeJpaEntity)가 있어 create-drop 이 FK 없는 테이블을 먼저 만든다.
        // 이 테스트가 보려는 건 job_position→employee FK 위반이므로, 그 판을 버리고 FK 를 건 최소 테이블로 갈아끼운다.
        em.createNativeQuery("DROP TABLE IF EXISTS employee").executeUpdate();
        em.createNativeQuery(
                "CREATE TABLE employee ("
                        + "user_id VARCHAR(20) PRIMARY KEY, "
                        + "job_position_id BIGINT, "
                        + "CONSTRAINT fk_emp_jp FOREIGN KEY (job_position_id) "
                        + "REFERENCES job_position(job_position_id))").executeUpdate();
        em.createNativeQuery(
                "INSERT INTO employee(user_id, job_position_id) VALUES ('EMP001', " + id + ")")
                .executeUpdate();

        assertThatThrownBy(() -> adapter.delete(saved))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
