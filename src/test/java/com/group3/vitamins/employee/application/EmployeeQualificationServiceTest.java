package com.group3.vitamins.employee.application;

import com.group3.vitamins.account.domain.TempPasswordGenerator;
import com.group3.vitamins.employee.application.command.CertificateItem;
import com.group3.vitamins.employee.application.command.EducationItem;
import com.group3.vitamins.employee.application.command.RegisterEmployeeCommand;
import com.group3.vitamins.employee.application.command.UpdateEmployeeCommand;
import com.group3.vitamins.employee.application.policy.EmployeeAdminPolicy;
import com.group3.vitamins.employee.application.port.AccountDeactivationPort;
import com.group3.vitamins.employee.application.port.CompanyCodeQueryPort;
import com.group3.vitamins.employee.application.port.EmployeeReferenceQueryPort;
import com.group3.vitamins.employee.application.port.InitialPasswordMailPort;
import com.group3.vitamins.employee.application.port.QualificationReferenceQueryPort;
import com.group3.vitamins.employee.application.service.EmployeeCommandService;
import com.group3.vitamins.employee.application.service.EmployeeRegistrationWriter;
import com.group3.vitamins.employee.domain.exception.EmployeeErrorCode;
import com.group3.vitamins.employee.domain.model.Degree;
import com.group3.vitamins.employee.domain.model.Employee;
import com.group3.vitamins.employee.domain.model.EmployeeCertificate;
import com.group3.vitamins.employee.domain.model.EmployeeEducation;
import com.group3.vitamins.employee.domain.repository.EmployeeCertificateRepository;
import com.group3.vitamins.employee.domain.repository.EmployeeEducationRepository;
import com.group3.vitamins.employee.domain.repository.EmployeeRepository;
import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.infrastructure.config.security.ThrottledPasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 사원 등록·수정에 딸린 학력/자격증(블록2) 검증. 기존 {@code EmployeeCommandServiceTest}(등록 골격)와
 * 분리해, 마스터 존재검사·전체 교체(생략/[] 구분)만 집중적으로 본다.
 */
@DisplayName("EmployeeCommandService 학력/자격증 통합")
class EmployeeQualificationServiceTest {

    private EmployeeRepository employeeRepository;
    private EmployeeReferenceQueryPort referenceQueryPort;
    private QualificationReferenceQueryPort qualificationReferenceQueryPort;
    private EmployeeEducationRepository educationRepository;
    private EmployeeCertificateRepository certificateRepository;
    private EmployeeRegistrationWriter registrationWriter;
    private TempPasswordGenerator tempPasswordGenerator;
    private ThrottledPasswordEncoder passwordEncoder;
    private CompanyCodeQueryPort companyCodeQueryPort;
    private EmployeeCommandService service;

    @BeforeEach
    void setUp() {
        employeeRepository = Mockito.mock(EmployeeRepository.class);
        referenceQueryPort = Mockito.mock(EmployeeReferenceQueryPort.class);
        qualificationReferenceQueryPort = Mockito.mock(QualificationReferenceQueryPort.class);
        educationRepository = Mockito.mock(EmployeeEducationRepository.class);
        certificateRepository = Mockito.mock(EmployeeCertificateRepository.class);
        registrationWriter = Mockito.mock(EmployeeRegistrationWriter.class);
        tempPasswordGenerator = Mockito.mock(TempPasswordGenerator.class);
        passwordEncoder = Mockito.mock(ThrottledPasswordEncoder.class);
        companyCodeQueryPort = Mockito.mock(CompanyCodeQueryPort.class);
        when(companyCodeQueryPort.findCodeByCompanyId(any())).thenReturn("vitas");
        CurrentCompanyIdProvider currentCompanyIdProvider = Mockito.mock(CurrentCompanyIdProvider.class);
        when(currentCompanyIdProvider.currentCompanyId()).thenReturn(1L);

        service = new EmployeeCommandService(new EmployeeAdminPolicy(), employeeRepository,
                referenceQueryPort, qualificationReferenceQueryPort, educationRepository, certificateRepository,
                registrationWriter, tempPasswordGenerator, passwordEncoder,
                Mockito.mock(InitialPasswordMailPort.class),
                Mockito.mock(AccountDeactivationPort.class),
                companyCodeQueryPort, currentCompanyIdProvider,
                Mockito.mock(DomainEventPublisher.class));
    }

    private void stubRegisterHappyPath() {
        when(employeeRepository.existsById(anyString())).thenReturn(false);
        when(referenceQueryPort.departmentExists(any(), anyLong())).thenReturn(true);
        when(referenceQueryPort.jobPositionExists(any(), anyLong())).thenReturn(true);
        when(tempPasswordGenerator.generate()).thenReturn("RAW-PW");
        when(passwordEncoder.encode("RAW-PW")).thenReturn("ENC-PW");
    }

    private RegisterEmployeeCommand registerCmd(List<EducationItem> educations, List<CertificateItem> certificates) {
        return new RegisterEmployeeCommand(
                "ADMIN", "EMP021", "홍길동", 2L, "2026-08-05", "MEMBER", 10L, "a@b.com", "010-1234-5678",
                educations, certificates);
    }

    // ── 등록 ──

    @Test
    @DisplayName("등록 — 학력·자격증이 마스터에 있으면 사원과 함께 저장한다(회사 스탬핑·학위 enum 변환)")
    void registerWithValidQualifications() {
        stubRegisterHappyPath();
        when(qualificationReferenceQueryPort.findExistingMajorIds(any(), eq(1L))).thenReturn(Set.of(3L));
        when(qualificationReferenceQueryPort.findExistingCertificateIds(any(), eq(1L))).thenReturn(Set.of(7L));

        service.register(registerCmd(
                List.of(new EducationItem(3L, "bachelor", " 한국대학교 ")),   // 소문자·공백 → 정규화
                List.of(new CertificateItem(7L, "2023-05-20"))));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EmployeeEducation>> eduCaptor = ArgumentCaptor.forClass(List.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EmployeeCertificate>> certCaptor = ArgumentCaptor.forClass(List.class);
        verify(registrationWriter).register(any(), eq("MEMBER"), eq("ENC-PW"),
                eduCaptor.capture(), certCaptor.capture());

        EmployeeEducation edu = eduCaptor.getValue().get(0);
        assertThat(edu.companyId()).isEqualTo(1L);
        assertThat(edu.userId()).isEqualTo("vitas-EMP021");
        assertThat(edu.majorId()).isEqualTo(3L);
        assertThat(edu.degree()).isEqualTo(Degree.BACHELOR);
        assertThat(edu.school()).isEqualTo("한국대학교");

        EmployeeCertificate cert = certCaptor.getValue().get(0);
        assertThat(cert.certificateId()).isEqualTo(7L);
        assertThat(cert.acquiredDate()).isEqualTo(LocalDate.parse("2023-05-20"));
    }

    @Test
    @DisplayName("등록 — 전공이 마스터에 없으면 MAJOR_NOT_FOUND (저장하지 않는다)")
    void registerRejectsUnknownMajor() {
        stubRegisterHappyPath();
        when(qualificationReferenceQueryPort.findExistingMajorIds(any(), eq(1L))).thenReturn(Set.of()); // 없음

        assertThatThrownBy(() -> service.register(registerCmd(
                List.of(new EducationItem(999L, "MASTER", null)), List.of())))
                .satisfies(hasCode(EmployeeErrorCode.MAJOR_NOT_FOUND));
        verify(registrationWriter, never()).register(any(), anyString(), anyString(), any(), any());
    }

    @Test
    @DisplayName("등록 — 자격증이 마스터에 없으면 CERT_NOT_FOUND")
    void registerRejectsUnknownCertificate() {
        stubRegisterHappyPath();
        when(qualificationReferenceQueryPort.findExistingCertificateIds(any(), eq(1L))).thenReturn(Set.of());

        assertThatThrownBy(() -> service.register(registerCmd(
                List.of(), List.of(new CertificateItem(999L, null))))) // 취득일 생략은 허용
                .satisfies(hasCode(EmployeeErrorCode.CERT_NOT_FOUND));
    }

    @Test
    @DisplayName("등록 — 전공 ID 누락은 EMP_INVALID_REQUEST (마스터 조회 전에 막힌다)")
    void registerRejectsMissingMajorId() {
        stubRegisterHappyPath();

        assertThatThrownBy(() -> service.register(registerCmd(
                List.of(new EducationItem(null, "BACHELOR", "한국대")), List.of())))
                .satisfies(hasCode(EmployeeErrorCode.EMP_INVALID_REQUEST));
    }

    @Test
    @DisplayName("등록 — 지원하지 않는 학위 값은 EMP_INVALID_REQUEST")
    void registerRejectsBadDegree() {
        stubRegisterHappyPath();
        when(qualificationReferenceQueryPort.findExistingMajorIds(any(), eq(1L))).thenReturn(Set.of(3L));

        assertThatThrownBy(() -> service.register(registerCmd(
                List.of(new EducationItem(3L, "PHD", "한국대")), List.of())))
                .satisfies(hasCode(EmployeeErrorCode.EMP_INVALID_REQUEST));
    }

    // ── 수정 (전체 교체) ──

    private Employee existingEmployee() {
        // 회사 1 소속, 시스템 아님, 재직 중
        return Employee.register("vitas-EMP001", "홍길동", 2L, 10L, "a@b.com", "010-1", LocalDate.parse("2024-01-01"), 1L);
    }

    private UpdateEmployeeCommand updateCmd(boolean eduProvided, List<EducationItem> educations) {
        // 학력만 시험. 다른 필드는 미전송, 자격증도 미전송.
        return new UpdateEmployeeCommand("ADMIN", "vitas-EMP001",
                false, null, false, null, false, null, false, null, false, null, false, null,
                eduProvided, educations, false, null);
    }

    @Test
    @DisplayName("수정 — 학력 배열([] 아님)을 보내면 기존을 지우고 새로 넣는다(전체 교체)")
    void updateReplacesEducations() {
        when(employeeRepository.findById("vitas-EMP001")).thenReturn(java.util.Optional.of(existingEmployee()));
        when(qualificationReferenceQueryPort.findExistingMajorIds(any(), eq(1L))).thenReturn(Set.of(3L));

        service.updateEmployee(updateCmd(true, List.of(new EducationItem(3L, "DOCTOR", "대학원"))));

        verify(educationRepository).deleteByUserId("vitas-EMP001");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EmployeeEducation>> captor = ArgumentCaptor.forClass(List.class);
        verify(educationRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement()
                .satisfies(e -> assertThat(e.degree()).isEqualTo(Degree.DOCTOR));
    }

    @Test
    @DisplayName("수정 — 빈 배열([])은 전부 삭제만 한다(마스터 조회 없음)")
    void updateEmptyArrayDeletesAll() {
        when(employeeRepository.findById("vitas-EMP001")).thenReturn(java.util.Optional.of(existingEmployee()));

        service.updateEmployee(updateCmd(true, List.of()));

        verify(educationRepository).deleteByUserId("vitas-EMP001");
        verify(educationRepository).saveAll(List.of());
        verify(qualificationReferenceQueryPort, never()).findExistingMajorIds(any(), anyLong());
    }

    @Test
    @DisplayName("수정 — 학력 미전송(provided=false)이면 학력을 건드리지 않는다(다른 필드만 수정)")
    void updateWithoutEducationsLeavesThemUntouched() {
        when(employeeRepository.findById("vitas-EMP001")).thenReturn(java.util.Optional.of(existingEmployee()));

        // 이름만 수정, 학력·자격증 미전송
        service.updateEmployee(new UpdateEmployeeCommand("ADMIN", "vitas-EMP001",
                true, "새이름", false, null, false, null, false, null, false, null, false, null,
                false, null, false, null));

        verify(educationRepository, never()).deleteByUserId(anyString());
        verify(educationRepository, never()).saveAll(any());
        verify(certificateRepository, never()).deleteByUserId(anyString());
    }

    private Consumer<Throwable> hasCode(Object expected) {
        return throwable -> assertThat(throwable)
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
