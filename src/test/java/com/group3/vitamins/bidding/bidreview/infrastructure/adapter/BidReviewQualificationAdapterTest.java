package com.group3.vitamins.bidding.bidreview.infrastructure.adapter;

import com.group3.vitamins.bidding.bidreview.application.port.BidReviewQualificationPort.NameCount;
import com.group3.vitamins.certificate.infrastructure.persistence.CertificateJpaEntity;
import com.group3.vitamins.employee.domain.model.Degree;
import com.group3.vitamins.employee.infrastructure.persistence.EmployeeCertificateJpaEntity;
import com.group3.vitamins.employee.infrastructure.persistence.EmployeeEducationJpaEntity;
import com.group3.vitamins.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.group3.vitamins.major.infrastructure.persistence.MajorJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:bid-review-qualification;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.hikari.connection-init-sql=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(BidReviewQualificationAdapter.class)
@DisplayName("BidReviewQualificationAdapter 보유 전공·학력·자격증 집계")
class BidReviewQualificationAdapterTest {

    private static final Long COMPANY_ID = 10L;
    private static final Long OTHER_COMPANY_ID = 20L;

    @Autowired
    private BidReviewQualificationAdapter adapter;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("전공별 인원수를 재직 중·비시스템·현재 회사 기준으로만 집계한다")
    void summarizesMajorsForActiveEmployeesOnly() {
        Long computerScience = seedMajor("컴퓨터공학");
        Long electrical = seedMajor("전기공학");

        seedEmployee("EMP001", false, null);
        seedEmployee("EMP002", false, null);
        seedEmployee("EMP003", false, LocalDate.of(2026, 1, 1)); // 퇴사 - 제외
        seedEmployee("EMP004", true, null); // 시스템 계정 - 제외
        seedEmployee("EMP999", false, null, OTHER_COMPANY_ID); // 다른 회사 - 제외

        seedEducation("EMP001", computerScience, Degree.BACHELOR);
        seedEducation("EMP002", computerScience, Degree.MASTER);
        seedEducation("EMP003", computerScience, Degree.BACHELOR);
        seedEducation("EMP004", computerScience, Degree.BACHELOR);
        seedEducation("EMP001", electrical, Degree.MASTER);
        seedEducationForCompany("EMP999", computerScience, Degree.BACHELOR, OTHER_COMPANY_ID);

        List<NameCount> result = adapter.summarizeMajors(COMPANY_ID);

        assertThat(result).containsExactly(
                new NameCount("전기공학", 1L),
                new NameCount("컴퓨터공학", 2L)
        );
    }

    @Test
    @DisplayName("한 사원이 같은 전공으로 여러 학력을 등록해도 1명으로만 센다")
    void deduplicatesEmployeeAcrossMultipleEducationsInSameMajor() {
        Long computerScience = seedMajor("컴퓨터공학");
        seedEmployee("EMP001", false, null);

        seedEducation("EMP001", computerScience, Degree.BACHELOR);
        seedEducation("EMP001", computerScience, Degree.MASTER);

        List<NameCount> result = adapter.summarizeMajors(COMPANY_ID);

        assertThat(result).containsExactly(new NameCount("컴퓨터공학", 1L));
    }

    @Test
    @DisplayName("학위별 인원수를 집계한다 - 전공과 교차하지 않는다")
    void summarizesDegreesIndependentlyOfMajor() {
        Long computerScience = seedMajor("컴퓨터공학");
        Long electrical = seedMajor("전기공학");
        seedEmployee("EMP001", false, null);
        seedEmployee("EMP002", false, null);

        seedEducation("EMP001", computerScience, Degree.BACHELOR);
        seedEducation("EMP002", electrical, Degree.BACHELOR);

        List<NameCount> result = adapter.summarizeDegrees(COMPANY_ID);

        assertThat(result).containsExactly(new NameCount("학사", 2L));
    }

    @Test
    @DisplayName("자격증별 인원수를 재직 중·비시스템·현재 회사 기준으로만 집계한다")
    void summarizesCertificatesForActiveEmployeesOnly() {
        Long infoProcessing = seedCertificate("정보처리기사");
        seedEmployee("EMP001", false, null);
        seedEmployee("EMP002", false, LocalDate.of(2026, 1, 1)); // 퇴사 - 제외

        seedCertificateHolding("EMP001", infoProcessing, LocalDate.of(2020, 1, 1));
        seedCertificateHolding("EMP002", infoProcessing, LocalDate.of(2020, 1, 1));

        List<NameCount> result = adapter.summarizeCertificates(COMPANY_ID);

        assertThat(result).containsExactly(new NameCount("정보처리기사", 1L));
    }

    @Test
    @DisplayName("등록된 전공·자격증이 없으면 빈 목록을 반환한다")
    void returnsEmptyWhenNothingRegistered() {
        assertThat(adapter.summarizeMajors(COMPANY_ID)).isEmpty();
        assertThat(adapter.summarizeDegrees(COMPANY_ID)).isEmpty();
        assertThat(adapter.summarizeCertificates(COMPANY_ID)).isEmpty();
    }

    private Long seedMajor(String name) {
        MajorJpaEntity entity = entityManager.persistAndFlush(
                new MajorJpaEntity(null, COMPANY_ID, name));
        return entity.getMajorId();
    }

    private Long seedCertificate(String name) {
        CertificateJpaEntity entity = entityManager.persistAndFlush(
                new CertificateJpaEntity(null, COMPANY_ID, name));
        return entity.getCertificateId();
    }

    private void seedEmployee(String userId, boolean isSystem, LocalDate resignedAt) {
        seedEmployee(userId, isSystem, resignedAt, COMPANY_ID);
    }

    private void seedEmployee(String userId, boolean isSystem, LocalDate resignedAt, Long companyId) {
        entityManager.persistAndFlush(new EmployeeJpaEntity(
                userId, "테스트사원", isSystem, null, null,
                null, null, LocalDate.of(2020, 1, 1), resignedAt, companyId
        ));
    }

    private void seedEducation(String userId, Long majorId, Degree degree) {
        seedEducationForCompany(userId, majorId, degree, COMPANY_ID);
    }

    private void seedEducationForCompany(String userId, Long majorId, Degree degree, Long companyId) {
        entityManager.persistAndFlush(
                new EmployeeEducationJpaEntity(companyId, userId, majorId, degree, null));
    }

    private void seedCertificateHolding(String userId, Long certificateId, LocalDate acquiredDate) {
        entityManager.persistAndFlush(
                new EmployeeCertificateJpaEntity(COMPANY_ID, userId, certificateId, acquiredDate));
    }
}
