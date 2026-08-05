package com.group3.vitamins.employee.application.service;

import com.group3.vitamins.account.application.port.MailDeliveryException;
import com.group3.vitamins.account.domain.TempPasswordGenerator;
import com.group3.vitamins.account.domain.exception.AccountErrorCode;
import com.group3.vitamins.employee.application.command.RegisterEmployeeCommand;
import com.group3.vitamins.employee.application.command.ResignEmployeeCommand;
import com.group3.vitamins.employee.application.command.UpdateEmployeeCommand;
import com.group3.vitamins.employee.application.policy.EmployeeAdminPolicy;
import com.group3.vitamins.employee.application.port.AccountDeactivationPort;
import com.group3.vitamins.employee.application.port.EmployeeReferenceQueryPort;
import com.group3.vitamins.employee.application.port.InitialPasswordMailPort;
import com.group3.vitamins.employee.application.result.EmployeeRegisterResult;
import com.group3.vitamins.employee.application.result.EmployeeResignResult;
import com.group3.vitamins.employee.application.usecase.EmployeeCommandUseCase;
import com.group3.vitamins.employee.domain.exception.EmployeeErrorCode;
import com.group3.vitamins.employee.domain.model.Employee;
import com.group3.vitamins.employee.domain.repository.EmployeeRepository;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.global.infrastructure.config.security.ThrottledPasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.regex.Pattern;

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

    // DB 컬럼 폭과 동일한 길이 상한 (employee: user_id 20 · name 50 · email 100 · phone 20)
    private static final int MAX_USER_ID_LENGTH = 20;
    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_EMAIL_LENGTH = 100;
    private static final int MAX_PHONE_LENGTH = 20;

    // 형식만 거르는 가벼운 검사(RFC 전체 준수 아님) — "local@domain.tld" 최소 형태를 확인한다.
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final EmployeeAdminPolicy employeeAdminPolicy;
    private final EmployeeRepository employeeRepository;
    private final EmployeeReferenceQueryPort referenceQueryPort;
    private final EmployeeRegistrationWriter registrationWriter;
    private final TempPasswordGenerator tempPasswordGenerator;
    private final ThrottledPasswordEncoder passwordEncoder;
    private final InitialPasswordMailPort initialPasswordMailPort;
    private final AccountDeactivationPort accountDeactivationPort;

    @Override
    public EmployeeRegisterResult register(RegisterEmployeeCommand command) {
        employeeAdminPolicy.assertAdmin(command.actorRole());

        // ── 1 검증 ──
        // 길이 상한은 DB 컬럼 폭과 같다. 여기서 막지 않으면 INSERT 가 데이터 절단으로 터지고, 그 예외가
        // 아래 catch 에서 사번 중복(409)으로 오인 변환된다 — 값 초과를 EMP_INVALID_REQUEST(400)로 먼저 막는다.
        String userId = requiredWithMax(command.userId(), MAX_USER_ID_LENGTH);
        String name = requiredWithMax(command.name(), MAX_NAME_LENGTH);
        Long departmentId = command.departmentId();
        if (departmentId == null) {
            throw new ValidationException(EmployeeErrorCode.EMP_INVALID_REQUEST);
        }
        String role = validateAssignableRole(command.role());
        LocalDate hiredAt = parseRequiredDate(command.hiredAt());
        String email = validateEmail(normalize(command.email()));
        String phone = optionalWithMax(normalize(command.phone()), MAX_PHONE_LENGTH);

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
                userId, name, departmentId, command.jobPositionId(), email, phone, hiredAt);
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
                // 원인 예외를 함께 남긴다 — SMTP 인증 실패·타임아웃·수신 거부를 구분해야 재발송을 판단할 수 있다.
                log.warn("초기 비밀번호 메일 발송 실패 - userId={}", userId, e);
            }
        }

        log.info("사원 등록 - userId={} emailRegistered={} emailSent={}", userId, emailRegistered, emailSent);
        return new EmployeeRegisterResult(userId, name, emailRegistered, emailSent);
    }

    /**
     * 사원 정보 수정 (`employee.md` §4). 전달한 필드만 병합해 UPDATE 한다. 해싱이 없어 메서드 전체를 트랜잭션으로
     * 묶는다 — findById 로 얻은 엔티티가 관리 상태를 유지해 UPDATE 가 깔끔하다.
     */
    @Override
    @Transactional
    public void updateEmployee(UpdateEmployeeCommand command) {
        employeeAdminPolicy.assertAdmin(command.actorRole());

        if (command.hasNoFields()) {
            throw new ValidationException(EmployeeErrorCode.EMP_INVALID_REQUEST);
        }

        Employee current = employeeRepository.findById(command.userId())
                .orElseThrow(() -> new NotFoundException(EmployeeErrorCode.EMP_NOT_FOUND));
        if (current.isSystem()) {
            throw new ForbiddenException(AccountErrorCode.ACC_SYSTEM_ACCOUNT_NOT_ALLOWED);
        }

        // 전달한 필드만 새 값으로, 나머지는 현재값 유지. jobPositionId 는 명시적 null = 직급 미지정.
        String name = current.getName();
        if (command.nameProvided()) {
            name = required(command.name()); // NOT NULL — 빈 값이면 EMP_INVALID_REQUEST
        }
        String phone = command.phoneProvided() ? normalize(command.phone()) : current.getPhone();
        String email = command.emailProvided() ? normalize(command.email()) : current.getEmail();

        Long departmentId = current.getDepartmentId();
        if (command.departmentIdProvided()) {
            departmentId = command.departmentId();
            if (departmentId != null && !referenceQueryPort.departmentExists(departmentId)) {
                throw new NotFoundException(EmployeeErrorCode.EMP_DEPARTMENT_NOT_FOUND);
            }
        }

        Long jobPositionId = current.getJobPositionId();
        if (command.jobPositionIdProvided()) {
            jobPositionId = command.jobPositionId(); // null = 직급 미지정으로 변경
            if (jobPositionId != null && !referenceQueryPort.jobPositionExists(jobPositionId)) {
                throw new NotFoundException(EmployeeErrorCode.EMP_JOB_POSITION_NOT_FOUND);
            }
        }

        LocalDate hiredAt = current.getHiredAt();
        if (command.hiredAtProvided()) {
            hiredAt = parseRequiredDate(command.hiredAt());
        }

        employeeRepository.update(current.withInfo(name, phone, email, departmentId, jobPositionId, hiredAt));
        log.info("사원 수정 - userId={}", command.userId());
    }

    /**
     * 퇴사 처리 (`employee.md` §5). 사원 정보는 지우지 않고 퇴사일만 기록하며, 계정을 함께 {@code INACTIVE} 로
     * 바꾼다(별도 상태변경 API 를 호출하지 않는다). 두 쓰기가 한 트랜잭션이다.
     */
    @Override
    @Transactional
    public EmployeeResignResult resignEmployee(ResignEmployeeCommand command) {
        employeeAdminPolicy.assertAdmin(command.actorRole());

        Employee current = employeeRepository.findById(command.userId())
                .orElseThrow(() -> new NotFoundException(EmployeeErrorCode.EMP_NOT_FOUND));
        if (current.isSystem()) {
            throw new ForbiddenException(AccountErrorCode.ACC_SYSTEM_ACCOUNT_NOT_ALLOWED);
        }
        if (current.isResigned()) {
            throw new ValidationException(EmployeeErrorCode.EMP_ALREADY_RESIGNED);
        }
        LocalDate resignedAt = parseRequiredDate(command.resignedAt());

        employeeRepository.update(current.resigned(resignedAt));
        accountDeactivationPort.deactivate(command.userId());

        log.info("사원 퇴사 - userId={} resignedAt={}", command.userId(), resignedAt);
        return new EmployeeResignResult(command.userId(), resignedAt.toString(), "INACTIVE");
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
            throw new ValidationException(EmployeeErrorCode.EMP_INVALID_REQUEST, e);
        }
    }

    private String required(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new ValidationException(EmployeeErrorCode.EMP_INVALID_REQUEST);
        }
        return normalized;
    }

    /** 필수값 + 최대 길이 검증. 비었거나 상한 초과면 EMP_INVALID_REQUEST. */
    private String requiredWithMax(String value, int maxLength) {
        String normalized = required(value);
        if (normalized.length() > maxLength) {
            throw new ValidationException(EmployeeErrorCode.EMP_INVALID_REQUEST);
        }
        return normalized;
    }

    /** 선택값 최대 길이 검증. null 은 그대로, 상한 초과면 EMP_INVALID_REQUEST. */
    private String optionalWithMax(String value, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new ValidationException(EmployeeErrorCode.EMP_INVALID_REQUEST);
        }
        return value;
    }

    /** 이메일 선택값 검증 — null 은 허용, 있으면 길이·형식을 본다. 잘못되면 EMP_INVALID_REQUEST. */
    private String validateEmail(String email) {
        if (email == null) {
            return null;
        }
        if (email.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException(EmployeeErrorCode.EMP_INVALID_REQUEST);
        }
        return email;
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
