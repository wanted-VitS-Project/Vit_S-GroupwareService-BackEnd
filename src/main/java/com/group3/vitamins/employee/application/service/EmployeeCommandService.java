package com.group3.vitamins.employee.application.service;

import com.group3.vitamins.account.application.port.MailDeliveryException;
import com.group3.vitamins.account.domain.TempPasswordGenerator;
import com.group3.vitamins.employee.application.command.RegisterEmployeeCommand;
import com.group3.vitamins.employee.application.policy.EmployeeAdminPolicy;
import com.group3.vitamins.employee.application.port.EmployeeReferenceQueryPort;
import com.group3.vitamins.employee.application.port.InitialPasswordMailPort;
import com.group3.vitamins.employee.application.result.EmployeeRegisterResult;
import com.group3.vitamins.employee.application.usecase.EmployeeCommandUseCase;
import com.group3.vitamins.employee.domain.exception.EmployeeErrorCode;
import com.group3.vitamins.employee.domain.model.Employee;
import com.group3.vitamins.employee.domain.repository.EmployeeRepository;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.global.infrastructure.config.security.ThrottledPasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;

/**
 * 사원 쓰기 유스케이스 (`employee.md` §3~) — 등록. 전부 ADMIN 전용이다.
 *
 * <p>등록은 사원과 계정을 함께 만든다({@code ACC-002}). 처리 순서가 곧 트랜잭션 경계다:
 * <ol>
 *   <li>검증 — 권한·필수값·role(ADMIN 불가)·사번 중복·부서/직급 존재</li>
 *   <li>초기 비밀번호 생성 + 해싱 — <b>트랜잭션 밖</b> (Argon2 64MB 로 DB 커넥션을 잡지 않는다)</li>
 *   <li>DB 반영 — {@link EmployeeRegistrationWriter} 가 사원+계정을 한 트랜잭션으로 커밋</li>
 *   <li>메일 발송 — <b>커밋 후</b>. 이메일이 있을 때만. 실패해도 등록은 성공(201)이며 {@code emailSent=false}</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeCommandService implements EmployeeCommandUseCase {

    private static final Set<String> ASSIGNABLE_ROLES = Set.of("MASTER", "MEMBER");

    private final EmployeeAdminPolicy employeeAdminPolicy;
    private final EmployeeRepository employeeRepository;
    private final EmployeeReferenceQueryPort referenceQueryPort;
    private final EmployeeRegistrationWriter registrationWriter;
    private final TempPasswordGenerator tempPasswordGenerator;
    private final ThrottledPasswordEncoder passwordEncoder;
    private final InitialPasswordMailPort initialPasswordMailPort;

    @Override
    public EmployeeRegisterResult register(RegisterEmployeeCommand command) {
        employeeAdminPolicy.assertAdmin(command.actorRole());

        // ── 1 검증 ──
        String userId = required(command.userId());
        String name = required(command.name());
        Long departmentId = command.departmentId();
        if (departmentId == null) {
            throw new ValidationException(EmployeeErrorCode.EMP_INVALID_REQUEST);
        }
        String role = validateAssignableRole(command.role());
        LocalDate hiredAt = parseRequiredDate(command.hiredAt());
        String email = normalize(command.email());

        if (employeeRepository.existsById(userId)) {
            throw new ConflictException(EmployeeErrorCode.EMP_USER_ID_DUPLICATED);
        }
        if (!referenceQueryPort.departmentExists(departmentId)) {
            throw new NotFoundException(EmployeeErrorCode.EMP_DEPARTMENT_NOT_FOUND);
        }
        if (command.jobPositionId() != null && !referenceQueryPort.jobPositionExists(command.jobPositionId())) {
            throw new NotFoundException(EmployeeErrorCode.EMP_JOB_POSITION_NOT_FOUND);
        }

        // ── 2 초기 비밀번호 생성 + 해싱 (트랜잭션 밖) ──
        // 이메일이 없어도 계정에는 비밀번호가 필요하므로 항상 발급한다 — 다만 전달할 수 없어 로그인만 불가하다.
        String rawPassword = tempPasswordGenerator.generate();
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // ── 3 DB 반영 (한 트랜잭션) ──
        Employee employee = Employee.register(
                userId, name, departmentId, command.jobPositionId(), email, normalize(command.phone()), hiredAt);
        try {
            registrationWriter.register(employee, role, encodedPassword);
        } catch (DataIntegrityViolationException e) {
            // 사전 존재 검사와 INSERT 사이의 레이스로 PK/UNIQUE 가 늦게 터진 경우.
            throw new ConflictException(EmployeeErrorCode.EMP_USER_ID_DUPLICATED, e);
        }

        // ── 4 메일 발송 (커밋 후) ──
        boolean emailRegistered = employee.hasEmail();
        boolean emailSent = false;
        if (emailRegistered) {
            try {
                initialPasswordMailPort.sendInitialPassword(email, name, rawPassword);
                emailSent = true;
            } catch (MailDeliveryException e) {
                // 사원·계정은 이미 만들어졌다. 비밀번호만 다시 보내면 되므로 등록은 성공으로 둔다.
                log.warn("초기 비밀번호 메일 발송 실패 - userId={}", userId);
            }
        }

        log.info("사원 등록 - userId={} emailRegistered={} emailSent={}", userId, emailRegistered, emailSent);
        return new EmployeeRegisterResult(userId, name, emailRegistered, emailSent);
    }

    /** ADMIN 은 부여 불가(전용 코드), 그 외 허용값이 아니면 형식 오류. */
    private String validateAssignableRole(String role) {
        String normalized = normalize(role);
        if ("ADMIN".equals(normalized)) {
            throw new ValidationException(EmployeeErrorCode.EMP_ADMIN_ROLE_NOT_ALLOWED);
        }
        if (normalized == null || !ASSIGNABLE_ROLES.contains(normalized)) {
            throw new ValidationException(EmployeeErrorCode.EMP_INVALID_REQUEST);
        }
        return normalized;
    }

    private LocalDate parseRequiredDate(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new ValidationException(EmployeeErrorCode.EMP_INVALID_REQUEST);
        }
        try {
            return LocalDate.parse(normalized); // ISO yyyy-MM-dd
        } catch (DateTimeParseException e) {
            throw new ValidationException(EmployeeErrorCode.EMP_INVALID_REQUEST);
        }
    }

    private String required(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new ValidationException(EmployeeErrorCode.EMP_INVALID_REQUEST);
        }
        return normalized;
    }

    /** 앞뒤 공백 제거 후 빈 문자열은 null 로 눕힌다. */
    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
