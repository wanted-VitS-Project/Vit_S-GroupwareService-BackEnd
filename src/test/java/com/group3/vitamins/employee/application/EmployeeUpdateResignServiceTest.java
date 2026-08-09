package com.group3.vitamins.employee.application;

import com.group3.vitamins.account.domain.TempPasswordGenerator;
import com.group3.vitamins.account.domain.exception.AccountErrorCode;
import com.group3.vitamins.employee.application.command.ResignEmployeeCommand;
import com.group3.vitamins.employee.application.command.UpdateEmployeeCommand;
import com.group3.vitamins.employee.application.policy.EmployeeAdminPolicy;
import com.group3.vitamins.employee.application.port.AccountDeactivationPort;
import com.group3.vitamins.employee.application.port.EmployeeReferenceQueryPort;
import com.group3.vitamins.employee.application.port.InitialPasswordMailPort;
import com.group3.vitamins.employee.application.result.EmployeeResignResult;
import com.group3.vitamins.employee.application.service.EmployeeCommandService;
import com.group3.vitamins.employee.application.service.EmployeeRegistrationWriter;
import com.group3.vitamins.employee.domain.exception.EmployeeErrorCode;
import com.group3.vitamins.employee.domain.model.Employee;
import com.group3.vitamins.employee.domain.repository.EmployeeRepository;
import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.infrastructure.config.security.ThrottledPasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("EmployeeCommandService 사원 수정·퇴사")
class EmployeeUpdateResignServiceTest {

    private EmployeeRepository employeeRepository;
    private EmployeeReferenceQueryPort referenceQueryPort;
    private AccountDeactivationPort accountDeactivationPort;
    private EmployeeCommandService service;

    @BeforeEach
    void setUp() {
        employeeRepository = Mockito.mock(EmployeeRepository.class);
        referenceQueryPort = Mockito.mock(EmployeeReferenceQueryPort.class);
        accountDeactivationPort = Mockito.mock(AccountDeactivationPort.class);
        // 등록 경로 협력자는 이 테스트에서 안 쓰므로 목만 채운다.
        service = new EmployeeCommandService(
                new EmployeeAdminPolicy(), employeeRepository, referenceQueryPort,
                Mockito.mock(EmployeeRegistrationWriter.class), Mockito.mock(TempPasswordGenerator.class),
                Mockito.mock(ThrottledPasswordEncoder.class), Mockito.mock(InitialPasswordMailPort.class),
                accountDeactivationPort,
                Mockito.mock(com.group3.vitamins.employee.application.port.CompanyCodeQueryPort.class));
    }

    private Employee active() {
        return Employee.restore("EMP021", "홍길동", false, 2L, 10L,
                "hong@vitamins.com", "010-1111-2222", LocalDate.of(2024, 3, 2), null, 1L);
    }

    @Nested
    @DisplayName("수정")
    class Update {

        private UpdateEmployeeCommand onlyName(String actorRole, String name) {
            return new UpdateEmployeeCommand(actorRole, "EMP021",
                    true, name, false, null, false, null,
                    false, null, false, null, false, null);
        }

        @Test
        @DisplayName("ADMIN 이 아니면 ACC_ADMIN_REQUIRED")
        void rejectsNonAdmin() {
            assertThatThrownBy(() -> service.updateEmployee(onlyName("MASTER", "새이름")))
                    .satisfies(hasCode(AccountErrorCode.ACC_ADMIN_REQUIRED));
            verifyNoInteractions(employeeRepository);
        }

        @Test
        @DisplayName("수정할 필드가 없으면 EMP_INVALID_REQUEST")
        void rejectsNoFields() {
            UpdateEmployeeCommand empty = new UpdateEmployeeCommand("ADMIN", "EMP021",
                    false, null, false, null, false, null, false, null, false, null, false, null);
            assertThatThrownBy(() -> service.updateEmployee(empty))
                    .satisfies(hasCode(EmployeeErrorCode.EMP_INVALID_REQUEST));
            verify(employeeRepository, never()).findById(anyString());
        }

        @Test
        @DisplayName("사원이 없으면 EMP_NOT_FOUND")
        void notFound() {
            when(employeeRepository.findById("EMP021")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.updateEmployee(onlyName("ADMIN", "새이름")))
                    .satisfies(hasCode(EmployeeErrorCode.EMP_NOT_FOUND));
        }

        @Test
        @DisplayName("시스템 계정은 ACC_SYSTEM_ACCOUNT_NOT_ALLOWED")
        void systemForbidden() {
            when(employeeRepository.findById("ADMIN001")).thenReturn(Optional.of(
                    Employee.restore("ADMIN001", "시스템", true, null, null, null, null, null, null, 1L)));
            UpdateEmployeeCommand cmd = new UpdateEmployeeCommand("ADMIN", "ADMIN001",
                    true, "x", false, null, false, null, false, null, false, null, false, null);
            assertThatThrownBy(() -> service.updateEmployee(cmd))
                    .satisfies(hasCode(AccountErrorCode.ACC_SYSTEM_ACCOUNT_NOT_ALLOWED));
            verify(employeeRepository, never()).updateInfo(any());
        }

        @Test
        @DisplayName("부서를 지정했는데 없으면 EMP_DEPARTMENT_NOT_FOUND")
        void departmentNotFound() {
            when(employeeRepository.findById("EMP021")).thenReturn(Optional.of(active()));
            when(referenceQueryPort.departmentExists(99L)).thenReturn(false);
            UpdateEmployeeCommand cmd = new UpdateEmployeeCommand("ADMIN", "EMP021",
                    false, null, false, null, false, null, true, 99L, false, null, false, null);
            assertThatThrownBy(() -> service.updateEmployee(cmd))
                    .satisfies(hasCode(EmployeeErrorCode.EMP_DEPARTMENT_NOT_FOUND));
        }

        @Test
        @DisplayName("전달한 필드만 바뀌고 나머지는 현재값을 유지한다")
        void partialUpdateKeepsOthers() {
            when(employeeRepository.findById("EMP021")).thenReturn(Optional.of(active()));

            service.updateEmployee(onlyName("ADMIN", "김철수"));

            ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
            verify(employeeRepository).updateInfo(captor.capture());
            Employee saved = captor.getValue();
            assertThat(saved.getName()).isEqualTo("김철수");
            assertThat(saved.getDepartmentId()).isEqualTo(2L);      // 유지
            assertThat(saved.getJobPositionId()).isEqualTo(10L);    // 유지
            assertThat(saved.getEmail()).isEqualTo("hong@vitamins.com"); // 유지
        }

        @Test
        @DisplayName("jobPositionId 에 null 을 보내면 직급이 지워진다 (존재 검사 안 함)")
        void clearsJobPosition() {
            when(employeeRepository.findById("EMP021")).thenReturn(Optional.of(active()));
            UpdateEmployeeCommand cmd = new UpdateEmployeeCommand("ADMIN", "EMP021",
                    false, null, false, null, false, null, false, null, true, null, false, null);

            service.updateEmployee(cmd);

            ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
            verify(employeeRepository).updateInfo(captor.capture());
            assertThat(captor.getValue().getJobPositionId()).isNull();
            verify(referenceQueryPort, never()).jobPositionExists(any());
        }

        @Test
        @DisplayName("이름이 50자를 넘으면 EMP_INVALID_REQUEST — 저장하지 않는다")
        void rejectsTooLongName() {
            when(employeeRepository.findById("EMP021")).thenReturn(Optional.of(active()));
            assertThatThrownBy(() -> service.updateEmployee(onlyName("ADMIN", "가".repeat(51))))
                    .satisfies(hasCode(EmployeeErrorCode.EMP_INVALID_REQUEST));
            verify(employeeRepository, never()).updateInfo(any());
        }

        @Test
        @DisplayName("이메일 형식이 올바르지 않으면 EMP_INVALID_REQUEST — 저장하지 않는다")
        void rejectsInvalidEmail() {
            when(employeeRepository.findById("EMP021")).thenReturn(Optional.of(active()));
            UpdateEmployeeCommand cmd = new UpdateEmployeeCommand("ADMIN", "EMP021",
                    false, null, false, null, true, "not-an-email",
                    false, null, false, null, false, null);
            assertThatThrownBy(() -> service.updateEmployee(cmd))
                    .satisfies(hasCode(EmployeeErrorCode.EMP_INVALID_REQUEST));
            verify(employeeRepository, never()).updateInfo(any());
        }

        @Test
        @DisplayName("연락처가 20자를 넘으면 EMP_INVALID_REQUEST — 저장하지 않는다")
        void rejectsTooLongPhone() {
            when(employeeRepository.findById("EMP021")).thenReturn(Optional.of(active()));
            UpdateEmployeeCommand cmd = new UpdateEmployeeCommand("ADMIN", "EMP021",
                    false, null, true, "0".repeat(21), false, null,
                    false, null, false, null, false, null);
            assertThatThrownBy(() -> service.updateEmployee(cmd))
                    .satisfies(hasCode(EmployeeErrorCode.EMP_INVALID_REQUEST));
            verify(employeeRepository, never()).updateInfo(any());
        }
    }

    @Nested
    @DisplayName("퇴사")
    class Resign {

        @Test
        @DisplayName("ADMIN 이 아니면 ACC_ADMIN_REQUIRED")
        void rejectsNonAdmin() {
            assertThatThrownBy(() -> service.resignEmployee(
                    new ResignEmployeeCommand("MEMBER", "EMP021", "2026-08-31")))
                    .satisfies(hasCode(AccountErrorCode.ACC_ADMIN_REQUIRED));
        }

        @Test
        @DisplayName("사원이 없으면 EMP_NOT_FOUND")
        void notFound() {
            when(employeeRepository.findById("EMP021")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.resignEmployee(
                    new ResignEmployeeCommand("ADMIN", "EMP021", "2026-08-31")))
                    .satisfies(hasCode(EmployeeErrorCode.EMP_NOT_FOUND));
        }

        @Test
        @DisplayName("이미 퇴사한 사원은 EMP_ALREADY_RESIGNED")
        void alreadyResigned() {
            when(employeeRepository.findById("EMP021")).thenReturn(Optional.of(
                    Employee.restore("EMP021", "홍길동", false, 2L, 10L, "h@v.com", null,
                            LocalDate.of(2024, 3, 2), LocalDate.of(2026, 1, 1), 1L)));
            assertThatThrownBy(() -> service.resignEmployee(
                    new ResignEmployeeCommand("ADMIN", "EMP021", "2026-08-31")))
                    .satisfies(hasCode(EmployeeErrorCode.EMP_ALREADY_RESIGNED));
            verify(employeeRepository, never()).resign(anyString(), any());
        }

        @Test
        @DisplayName("날짜 형식이 틀리면 EMP_INVALID_REQUEST")
        void badDate() {
            when(employeeRepository.findById("EMP021")).thenReturn(Optional.of(active()));
            assertThatThrownBy(() -> service.resignEmployee(
                    new ResignEmployeeCommand("ADMIN", "EMP021", "2026/08/31")))
                    .satisfies(hasCode(EmployeeErrorCode.EMP_INVALID_REQUEST));
        }

        @Test
        @DisplayName("정상 퇴사면 퇴사일 기록 + 계정 비활성화, accountStatus=INACTIVE")
        void resignsAndDeactivates() {
            when(employeeRepository.findById("EMP021")).thenReturn(Optional.of(active()));

            EmployeeResignResult result = service.resignEmployee(
                    new ResignEmployeeCommand("ADMIN", "EMP021", "2026-08-31"));

            verify(employeeRepository).resign("EMP021", LocalDate.of(2026, 8, 31));
            verify(accountDeactivationPort).deactivate("EMP021");
            assertThat(result.resignedAt()).isEqualTo("2026-08-31");
            assertThat(result.accountStatus()).isEqualTo("INACTIVE");
        }
    }

    private Consumer<Throwable> hasCode(Object expected) {
        return throwable -> assertThat(throwable)
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
