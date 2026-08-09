package com.group3.vitamins.employee.application;

import com.group3.vitamins.account.application.port.MailDeliveryException;
import com.group3.vitamins.account.domain.TempPasswordGenerator;
import com.group3.vitamins.employee.application.command.RegisterBulkCommand;
import com.group3.vitamins.employee.application.command.ValidateBulkCommand;
import com.group3.vitamins.employee.application.policy.EmployeeAdminPolicy;
import com.group3.vitamins.employee.application.port.EmployeeBulkReferenceQueryPort;
import com.group3.vitamins.employee.application.port.EmployeeExcelParserPort;
import com.group3.vitamins.employee.application.port.EmployeeExcelTemplatePort;
import com.group3.vitamins.employee.application.port.InitialPasswordMailPort;
import com.group3.vitamins.employee.application.result.BulkRegisterResult;
import com.group3.vitamins.employee.application.result.BulkValidateResult;
import com.group3.vitamins.employee.application.result.BulkValidation;
import com.group3.vitamins.employee.application.result.ParsedEmployeeRow;
import com.group3.vitamins.employee.application.service.EmployeeBulkService;
import com.group3.vitamins.employee.application.service.EmployeeRegistrationWriter;
import com.group3.vitamins.employee.domain.exception.EmployeeErrorCode;
import com.group3.vitamins.employee.domain.model.Employee;
import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.infrastructure.config.security.ThrottledPasswordEncoder;
import com.group3.vitamins.employee.application.port.CompanyCodeQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("EmployeeBulkService 엑셀 일괄 등록")
class EmployeeBulkServiceTest {

    private static final String ADMIN = "ADMIN";

    private EmployeeExcelTemplatePort templatePort;
    private EmployeeExcelParserPort parserPort;
    private EmployeeBulkReferenceQueryPort referencePort;
    private EmployeeRegistrationWriter registrationWriter;
    private TempPasswordGenerator tempPasswordGenerator;
    private ThrottledPasswordEncoder passwordEncoder;
    private InitialPasswordMailPort mailPort;
    private CompanyCodeQueryPort companyCodeQueryPort;
    private CurrentCompanyIdProvider currentCompanyIdProvider;
    private EmployeeBulkService service;

    @BeforeEach
    void setUp() {
        templatePort = Mockito.mock(EmployeeExcelTemplatePort.class);
        parserPort = Mockito.mock(EmployeeExcelParserPort.class);
        referencePort = Mockito.mock(EmployeeBulkReferenceQueryPort.class);
        registrationWriter = Mockito.mock(EmployeeRegistrationWriter.class);
        tempPasswordGenerator = Mockito.mock(TempPasswordGenerator.class);
        passwordEncoder = Mockito.mock(ThrottledPasswordEncoder.class);
        mailPort = Mockito.mock(InitialPasswordMailPort.class);
        companyCodeQueryPort = Mockito.mock(CompanyCodeQueryPort.class);
        currentCompanyIdProvider = Mockito.mock(CurrentCompanyIdProvider.class);
        service = new EmployeeBulkService(
                new EmployeeAdminPolicy(), templatePort, parserPort, referencePort,
                registrationWriter, tempPasswordGenerator, passwordEncoder, mailPort,
                companyCodeQueryPort, currentCompanyIdProvider);

        // 기본 스텁 — 개발팀/대리는 존재, 기존 사번 없음(빈 Set), 회사코드 vitas, 회사ID 1, 해싱·비번은 고정
        when(referencePort.resolveDepartmentIdsByName(any(), anyLong())).thenReturn(Map.of("개발팀", 10L));
        when(referencePort.resolveJobPositionIdsByName(any(), anyLong())).thenReturn(Map.of("대리", 5L));
        when(referencePort.findExistingUserIds(any())).thenReturn(Set.of());
        when(companyCodeQueryPort.findCodeByCompanyId(any())).thenReturn("vitas");
        when(currentCompanyIdProvider.currentCompanyId()).thenReturn(1L);
        when(tempPasswordGenerator.generate()).thenReturn("Temp1234!");
        when(passwordEncoder.encode(anyString())).thenReturn("HASHED");
    }

    // ---- 헬퍼 ---------------------------------------------------------------

    private ParsedEmployeeRow row(int n, String userId, String name, String dept, String pos,
                                  String hiredAt, String email, String role) {
        return new ParsedEmployeeRow(n, userId, name, dept, pos, hiredAt, email, null, role);
    }

    private ParsedEmployeeRow valid(int n, String userId, String email) {
        return row(n, userId, "홍길동", "개발팀", "대리", "2026-01-01", email, "MEMBER");
    }

    private ValidateBulkCommand validateCmd(List<ParsedEmployeeRow> rows) {
        when(parserPort.parse(any())).thenReturn(rows);
        return new ValidateBulkCommand(ADMIN, new byte[]{1}, "emp.xlsx", 100L);
    }

    private RegisterBulkCommand registerCmd(List<ParsedEmployeeRow> rows, boolean skipErrors) {
        when(parserPort.parse(any())).thenReturn(rows);
        return new RegisterBulkCommand(ADMIN, new byte[]{1}, "emp.xlsx", 100L, skipErrors);
    }

    private Consumer<Throwable> hasCode(Object expected) {
        return t -> assertThat(t).isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode()).isEqualTo(expected);
    }

    // ---- 권한 · 템플릿 ------------------------------------------------------

    @Test
    @DisplayName("ADMIN 이 아니면 ACC_ADMIN_REQUIRED (템플릿·검증·등록 공통)")
    void nonAdminRejected() {
        assertThatThrownBy(() -> service.getTemplate("MASTER"))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> service.validate(new ValidateBulkCommand("MASTER", new byte[]{1}, "a.xlsx", 1)))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> service.register(new RegisterBulkCommand("MASTER", new byte[]{1}, "a.xlsx", 1, true)))
                .isInstanceOf(DomainException.class);
        verify(parserPort, never()).parse(any());  // 권한에서 막혀 파싱까지 가지 않는다
    }

    @Test
    @DisplayName("템플릿은 포트가 만든 바이너리를 그대로 돌려준다")
    void template() {
        when(templatePort.generate()).thenReturn(new byte[]{1, 2, 3});
        assertThat(service.getTemplate(ADMIN)).containsExactly(1, 2, 3);
    }

    // ---- 파일 메타 (열기 전 400) --------------------------------------------

    @Nested
    @DisplayName("파일 메타")
    class FileMeta {

        @Test
        @DisplayName("파일이 없으면 EMP_FILE_REQUIRED")
        void required() {
            assertThatThrownBy(() -> service.validate(new ValidateBulkCommand(ADMIN, new byte[0], "a.xlsx", 0)))
                    .satisfies(hasCode(EmployeeErrorCode.EMP_FILE_REQUIRED));
        }

        @Test
        @DisplayName("엑셀 확장자가 아니면 EMP_FILE_TYPE_INVALID")
        void type() {
            assertThatThrownBy(() -> service.validate(new ValidateBulkCommand(ADMIN, new byte[]{1}, "a.csv", 10)))
                    .satisfies(hasCode(EmployeeErrorCode.EMP_FILE_TYPE_INVALID));
        }

        @Test
        @DisplayName("신고 size 가 5MB 를 초과하면 EMP_FILE_SIZE_EXCEEDED")
        void size() {
            long over = 5L * 1024 * 1024 + 1;
            assertThatThrownBy(() -> service.validate(new ValidateBulkCommand(ADMIN, new byte[]{1}, "a.xlsx", over)))
                    .satisfies(hasCode(EmployeeErrorCode.EMP_FILE_SIZE_EXCEEDED));
        }

        @Test
        @DisplayName("실제 content 길이가 5MB 를 초과하면 size 가 작아도 EMP_FILE_SIZE_EXCEEDED")
        void contentLengthExceeds() {
            byte[] big = new byte[5 * 1024 * 1024 + 1];
            assertThatThrownBy(() -> service.validate(new ValidateBulkCommand(ADMIN, big, "a.xlsx", 100L)))
                    .satisfies(hasCode(EmployeeErrorCode.EMP_FILE_SIZE_EXCEEDED));
        }

    }

    // ---- 검증 (§7) ----------------------------------------------------------

    @Nested
    @DisplayName("행별 검증")
    class Validation {

        @Test
        @DisplayName("유효 행은 통과하고 각 오류 유형을 정확히 분류한다")
        void classifiesErrors() {
            // DB 는 접두사 형태로 저장하므로 기존 사번 스텁도 접두사 형태로 준다 (row "EMP_DB" → "vitas-EMP_DB").
            when(referencePort.findExistingUserIds(any())).thenReturn(Set.of("vitas-EMP_DB"));
            List<ParsedEmployeeRow> rows = List.of(
                    valid(2, "EMP100", "a@b.com"),                                   // 유효
                    row(3, null, "김", "개발팀", null, "2026-01-01", null, "MEMBER"),  // 사번 누락
                    row(4, "EMP101", "이", "개발팀", null, "2026-01-01", null, "ADMIN"), // ADMIN 금지
                    row(5, "EMP102", "박", "없는팀", null, "2026-01-01", null, "MEMBER"),// 부서 없음
                    row(6, "EMP_DB", "최", "개발팀", null, "2026-01-01", null, "MEMBER"));// DB 기존

            BulkValidateResult r = service.validate(validateCmd(rows));

            assertThat(r.totalRows()).isEqualTo(5);
            assertThat(r.validCount()).isEqualTo(1);
            assertThat(r.errorCount()).isEqualTo(4);
            assertThat(r.errors()).extracting("validation").containsExactly(
                    BulkValidation.REQUIRED_COLUMN,
                    BulkValidation.ADMIN_ROLE_NOT_ALLOWED,
                    BulkValidation.DEPARTMENT_NOT_FOUND,
                    BulkValidation.USER_ID_DUPLICATED);
        }

        @Test
        @DisplayName("접두사 포함 20자 경계 — base 14자 유효, 15자는 REQUIRED_COLUMN")
        void userIdPrefixLengthBoundary() {
            List<ParsedEmployeeRow> rows = List.of(
                    valid(2, "EMP01234567890", "a@b.com"),   // base 14 → "vitas-"+14=20 유효
                    valid(3, "EMP012345678901", "b@c.com"));  // base 15 → 21 초과 → 거부
            BulkValidateResult r = service.validate(validateCmd(rows));

            assertThat(r.validCount()).isEqualTo(1);
            assertThat(r.errorCount()).isEqualTo(1);
            assertThat(r.errors()).extracting("validation").containsExactly(BulkValidation.REQUIRED_COLUMN);
        }

        @Test
        @DisplayName("파일 내 사번 중복은 두 행 모두 USER_ID_DUPLICATED, 최초 행을 가리킨다")
        void inFileDuplicate() {
            List<ParsedEmployeeRow> rows = List.of(
                    valid(2, "DUP", "a@b.com"),
                    valid(7, "DUP", "c@d.com"));

            BulkValidateResult r = service.validate(validateCmd(rows));

            assertThat(r.errorCount()).isEqualTo(2);
            assertThat(r.errors()).allSatisfy(e ->
                    assertThat(e.validation()).isEqualTo(BulkValidation.USER_ID_DUPLICATED));
            assertThat(r.errors().get(0).message()).contains("2행");
        }

        @Test
        @DisplayName("직급명이 없거나 불일치하면 오류가 아니라 유효 행으로 둔다(직급 null 등록)")
        void unknownJobPositionIsNotError() {
            List<ParsedEmployeeRow> rows = List.of(
                    row(2, "EMP100", "홍", "개발팀", "없는직급", "2026-01-01", "a@b.com", "MEMBER"));

            BulkValidateResult r = service.validate(validateCmd(rows));

            assertThat(r.errorCount()).isZero();
            assertThat(r.validCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("이메일 없는 유효 행 수를 센다 (emailNotRegisteredCount)")
        void emailNotRegisteredCount() {
            List<ParsedEmployeeRow> rows = List.of(
                    valid(2, "EMP100", "a@b.com"),
                    valid(3, "EMP101", null));

            BulkValidateResult r = service.validate(validateCmd(rows));

            assertThat(r.validCount()).isEqualTo(2);
            assertThat(r.emailNotRegisteredCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("입사일 형식 오류는 REQUIRED_COLUMN 버킷으로 분류한다")
        void badDate() {
            List<ParsedEmployeeRow> rows = List.of(
                    row(2, "EMP100", "홍", "개발팀", null, "2026/01/01", "a@b.com", "MEMBER"));

            BulkValidateResult r = service.validate(validateCmd(rows));

            assertThat(r.errorCount()).isEqualTo(1);
            assertThat(r.errors().get(0).validation()).isEqualTo(BulkValidation.REQUIRED_COLUMN);
        }
    }

    // ---- 등록 (§8) ----------------------------------------------------------

    @Nested
    @DisplayName("일괄 등록")
    class Registration {

        @Test
        @DisplayName("skipErrors=false 인데 오류 행이 있으면 EMP_HAS_ERRORS 로 막고 등록하지 않는다")
        void haltsOnErrors() {
            List<ParsedEmployeeRow> rows = List.of(
                    valid(2, "EMP100", "a@b.com"),
                    row(3, null, "김", "개발팀", null, "2026-01-01", null, "MEMBER")); // 오류 행

            assertThatThrownBy(() -> service.register(registerCmd(rows, false)))
                    .satisfies(hasCode(EmployeeErrorCode.EMP_HAS_ERRORS));
            verify(registrationWriter, never()).register(any(), anyString(), anyString());
        }

        @Test
        @DisplayName("skipErrors=true 면 유효 행만 등록하고 메일은 이메일 있는 행에만 보낸다")
        void partialRegister() {
            List<ParsedEmployeeRow> rows = List.of(
                    valid(2, "EMP100", "a@b.com"),                                    // 등록·메일
                    valid(3, "EMP101", null),                                          // 등록·이메일 없음
                    row(4, null, "김", "개발팀", null, "2026-01-01", null, "MEMBER")); // 오류(스킵)

            BulkRegisterResult r = service.register(registerCmd(rows, true));

            assertThat(r.totalRows()).isEqualTo(3);
            assertThat(r.registeredCount()).isEqualTo(2);
            assertThat(r.failedCount()).isEqualTo(1);          // 오류 1행
            assertThat(r.emailSentCount()).isEqualTo(1);
            assertThat(r.emailNotRegistered()).extracting("userId").containsExactly("vitas-EMP101");

            ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
            verify(registrationWriter, times(2)).register(captor.capture(), anyString(), anyString());
            assertThat(captor.getAllValues()).extracting(Employee::getCompanyId).containsOnly(1L); // 등록된 전원 회사 스탬핑
        }

        @Test
        @DisplayName("등록 중 사번 레이스로 무결성 위반이 나면 그 행만 실패 처리하고 계속한다")
        void raceFailureIsolated() {
            List<ParsedEmployeeRow> rows = List.of(
                    valid(2, "EMP100", "a@b.com"),
                    valid(3, "EMP101", "c@d.com"));
            // 두 번째 행에서만 무결성 위반
            Mockito.doNothing().doThrow(new DataIntegrityViolationException("dup"))
                    .when(registrationWriter).register(any(), anyString(), anyString());

            BulkRegisterResult r = service.register(registerCmd(rows, false));

            assertThat(r.registeredCount()).isEqualTo(1);
            assertThat(r.failedCount()).isEqualTo(1);
            assertThat(r.errors()).singleElement()
                    .satisfies(e -> assertThat(e.validation()).isEqualTo(BulkValidation.USER_ID_DUPLICATED));
        }

        @Test
        @DisplayName("메일 발송이 실패해도 등록은 성공으로 유지하고 emailSentCount 만 줄인다")
        void mailFailureDoesNotFailRegistration() {
            Mockito.doThrow(new MailDeliveryException(new RuntimeException("smtp down")))
                    .when(mailPort).sendInitialPassword(anyString(), anyString(), anyString());
            List<ParsedEmployeeRow> rows = List.of(valid(2, "EMP100", "a@b.com"));

            BulkRegisterResult r = service.register(registerCmd(rows, false));

            assertThat(r.registeredCount()).isEqualTo(1);   // 사원·계정은 만들어졌다
            assertThat(r.failedCount()).isZero();
            assertThat(r.emailSentCount()).isZero();        // 메일만 실패
            verify(registrationWriter, times(1)).register(any(), anyString(), anyString());
        }
    }
}
