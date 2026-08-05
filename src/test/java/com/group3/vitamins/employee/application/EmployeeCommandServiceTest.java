package com.group3.vitamins.employee.application;

import com.group3.vitamins.account.application.port.MailDeliveryException;
import com.group3.vitamins.account.domain.TempPasswordGenerator;
import com.group3.vitamins.account.domain.exception.AccountErrorCode;
import com.group3.vitamins.employee.application.command.RegisterEmployeeCommand;
import com.group3.vitamins.employee.application.policy.EmployeeAdminPolicy;
import com.group3.vitamins.employee.application.port.EmployeeReferenceQueryPort;
import com.group3.vitamins.employee.application.port.InitialPasswordMailPort;
import com.group3.vitamins.employee.application.result.EmployeeRegisterResult;
import com.group3.vitamins.employee.application.service.EmployeeCommandService;
import com.group3.vitamins.employee.application.service.EmployeeRegistrationWriter;
import com.group3.vitamins.employee.domain.exception.EmployeeErrorCode;
import com.group3.vitamins.employee.domain.model.Employee;
import com.group3.vitamins.employee.domain.repository.EmployeeRepository;
import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.infrastructure.config.security.ThrottledPasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("EmployeeCommandService 사원 등록")
class EmployeeCommandServiceTest {

    private EmployeeRepository employeeRepository;
    private EmployeeReferenceQueryPort referenceQueryPort;
    private EmployeeRegistrationWriter registrationWriter;
    private TempPasswordGenerator tempPasswordGenerator;
    private ThrottledPasswordEncoder passwordEncoder;
    private InitialPasswordMailPort mailPort;
    private EmployeeCommandService service;

    @BeforeEach
    void setUp() {
        employeeRepository = Mockito.mock(EmployeeRepository.class);
        referenceQueryPort = Mockito.mock(EmployeeReferenceQueryPort.class);
        registrationWriter = Mockito.mock(EmployeeRegistrationWriter.class);
        tempPasswordGenerator = Mockito.mock(TempPasswordGenerator.class);
        passwordEncoder = Mockito.mock(ThrottledPasswordEncoder.class);
        mailPort = Mockito.mock(InitialPasswordMailPort.class);
        service = new EmployeeCommandService(new EmployeeAdminPolicy(), employeeRepository,
                referenceQueryPort, registrationWriter, tempPasswordGenerator, passwordEncoder, mailPort);
    }

    /** email 있는 정상 등록의 스텁 (해피패스 계열이 공유). */
    private void stubHappyPath() {
        when(employeeRepository.existsById(anyString())).thenReturn(false);
        when(referenceQueryPort.departmentExists(any())).thenReturn(true);
        when(referenceQueryPort.jobPositionExists(any())).thenReturn(true);
        when(tempPasswordGenerator.generate()).thenReturn("RAW-PW");
        when(passwordEncoder.encode("RAW-PW")).thenReturn("ENC-PW");
    }

    private RegisterEmployeeCommand cmd(String role, String hiredAt, String email) {
        return new RegisterEmployeeCommand(
                "ADMIN", "EMP021", "홍길동", 2L, hiredAt, role, 10L, email, "010-1234-5678");
    }

    @Test
    @DisplayName("ADMIN 이 아니면 ACC_ADMIN_REQUIRED — 아무것도 하지 않는다")
    void rejectsNonAdmin() {
        RegisterEmployeeCommand command = new RegisterEmployeeCommand(
                "MASTER", "EMP021", "홍길동", 2L, "2026-08-05", "MEMBER", 10L, "a@b.com", null);

        assertThatThrownBy(() -> service.register(command))
                .satisfies(hasCode(AccountErrorCode.ACC_ADMIN_REQUIRED));
        verifyNoInteractions(employeeRepository, referenceQueryPort, registrationWriter);
    }

    @Test
    @DisplayName("role=ADMIN 은 EMP_ADMIN_ROLE_NOT_ALLOWED")
    void rejectsAdminRole() {
        assertThatThrownBy(() -> service.register(cmd("ADMIN", "2026-08-05", "a@b.com")))
                .satisfies(hasCode(EmployeeErrorCode.EMP_ADMIN_ROLE_NOT_ALLOWED));
        verify(registrationWriter, never()).register(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("허용되지 않는 role 값은 EMP_INVALID_REQUEST")
    void rejectsInvalidRole() {
        assertThatThrownBy(() -> service.register(cmd("SUPER", "2026-08-05", "a@b.com")))
                .satisfies(hasCode(EmployeeErrorCode.EMP_INVALID_REQUEST));
    }

    @Test
    @DisplayName("입사일 형식 오류는 EMP_INVALID_REQUEST")
    void rejectsBadDate() {
        assertThatThrownBy(() -> service.register(cmd("MEMBER", "2026/08/05", "a@b.com")))
                .satisfies(hasCode(EmployeeErrorCode.EMP_INVALID_REQUEST));
    }

    @Test
    @DisplayName("필수값(이름) 누락은 EMP_INVALID_REQUEST")
    void rejectsMissingName() {
        RegisterEmployeeCommand command = new RegisterEmployeeCommand(
                "ADMIN", "EMP021", "  ", 2L, "2026-08-05", "MEMBER", null, null, null);
        assertThatThrownBy(() -> service.register(command))
                .satisfies(hasCode(EmployeeErrorCode.EMP_INVALID_REQUEST));
    }

    @Test
    @DisplayName("사번 중복은 EMP_USER_ID_DUPLICATED — 저장하지 않는다")
    void rejectsDuplicate() {
        when(employeeRepository.existsById("EMP021")).thenReturn(true);

        assertThatThrownBy(() -> service.register(cmd("MEMBER", "2026-08-05", "a@b.com")))
                .satisfies(hasCode(EmployeeErrorCode.EMP_USER_ID_DUPLICATED));
        verify(registrationWriter, never()).register(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("부서가 없으면 EMP_DEPARTMENT_NOT_FOUND")
    void rejectsMissingDepartment() {
        when(employeeRepository.existsById("EMP021")).thenReturn(false);
        when(referenceQueryPort.departmentExists(2L)).thenReturn(false);

        assertThatThrownBy(() -> service.register(cmd("MEMBER", "2026-08-05", "a@b.com")))
                .satisfies(hasCode(EmployeeErrorCode.EMP_DEPARTMENT_NOT_FOUND));
    }

    @Test
    @DisplayName("직급을 지정했는데 없으면 EMP_JOB_POSITION_NOT_FOUND")
    void rejectsMissingJobPosition() {
        when(employeeRepository.existsById("EMP021")).thenReturn(false);
        when(referenceQueryPort.departmentExists(2L)).thenReturn(true);
        when(referenceQueryPort.jobPositionExists(10L)).thenReturn(false);

        assertThatThrownBy(() -> service.register(cmd("MEMBER", "2026-08-05", "a@b.com")))
                .satisfies(hasCode(EmployeeErrorCode.EMP_JOB_POSITION_NOT_FOUND));
    }

    @Test
    @DisplayName("이메일이 있으면 해시 후 사원+계정을 저장하고 초기 비밀번호를 발송한다")
    void registersWithEmail() {
        stubHappyPath();

        EmployeeRegisterResult result = service.register(cmd("MEMBER", "2026-08-05", "hong@vitamins.com"));

        // 저장은 트랜잭션 writer 로, 인코딩된 비밀번호와 role 이 넘어간다
        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(registrationWriter).register(employeeCaptor.capture(), eq("MEMBER"), eq("ENC-PW"));
        assertThat(employeeCaptor.getValue().getUserId()).isEqualTo("EMP021");
        assertThat(employeeCaptor.getValue().isSystem()).isFalse();
        // 원문 비밀번호로 메일 발송
        verify(mailPort).sendInitialPassword("hong@vitamins.com", "홍길동", "RAW-PW");
        assertThat(result.emailRegistered()).isTrue();
        assertThat(result.emailSent()).isTrue();
    }

    @Test
    @DisplayName("이메일이 없으면 계정은 만들되 메일은 보내지 않는다 (로그인 불가 상태)")
    void registersWithoutEmail() {
        stubHappyPath();

        EmployeeRegisterResult result = service.register(cmd("MEMBER", "2026-08-05", null));

        verify(registrationWriter).register(any(), eq("MEMBER"), eq("ENC-PW"));
        verifyNoInteractions(mailPort);
        assertThat(result.emailRegistered()).isFalse();
        assertThat(result.emailSent()).isFalse();
    }

    @Test
    @DisplayName("메일 발송이 실패해도 등록은 성공하고 emailSent=false")
    void mailFailureStillSucceeds() {
        stubHappyPath();
        doThrow(new MailDeliveryException(new RuntimeException("smtp down")))
                .when(mailPort).sendInitialPassword(anyString(), anyString(), anyString());

        EmployeeRegisterResult result = service.register(cmd("MEMBER", "2026-08-05", "hong@vitamins.com"));

        assertThat(result.emailRegistered()).isTrue();
        assertThat(result.emailSent()).isFalse();
    }

    @Test
    @DisplayName("저장 시 늦게 터진 UNIQUE 위반은 EMP_USER_ID_DUPLICATED 로 변환한다 (레이스)")
    void translatesLateUniqueViolation() {
        stubHappyPath();
        doThrow(new DataIntegrityViolationException("dup"))
                .when(registrationWriter).register(any(), anyString(), anyString());

        assertThatThrownBy(() -> service.register(cmd("MEMBER", "2026-08-05", "hong@vitamins.com")))
                .satisfies(hasCode(EmployeeErrorCode.EMP_USER_ID_DUPLICATED));
        verifyNoInteractions(mailPort);
    }

    private Consumer<Throwable> hasCode(Object expected) {
        return throwable -> assertThat(throwable)
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
