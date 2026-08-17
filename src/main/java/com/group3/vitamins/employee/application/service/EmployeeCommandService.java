package com.group3.vitamins.employee.application.service;

import com.group3.vitamins.account.application.port.MailDeliveryException;
import com.group3.vitamins.account.domain.TempPasswordGenerator;
import com.group3.vitamins.account.domain.exception.AccountErrorCode;
import com.group3.vitamins.employee.application.command.CertificateItem;
import com.group3.vitamins.employee.application.command.EducationItem;
import com.group3.vitamins.employee.application.command.RegisterEmployeeCommand;
import com.group3.vitamins.employee.application.command.ResignEmployeeCommand;
import com.group3.vitamins.employee.application.command.UpdateEmployeeCommand;
import com.group3.vitamins.employee.application.policy.EmployeeAdminPolicy;
import com.group3.vitamins.employee.application.port.AccountDeactivationPort;
import com.group3.vitamins.employee.application.port.CompanyCodeQueryPort;
import com.group3.vitamins.employee.application.port.EmployeeReferenceQueryPort;
import com.group3.vitamins.employee.application.port.InitialPasswordMailPort;
import com.group3.vitamins.employee.application.port.QualificationReferenceQueryPort;
import com.group3.vitamins.employee.application.result.EmployeeRegisterResult;
import com.group3.vitamins.employee.application.result.EmployeeResignResult;
import com.group3.vitamins.employee.application.usecase.EmployeeCommandUseCase;
import com.group3.vitamins.employee.domain.exception.EmployeeErrorCode;
import com.group3.vitamins.employee.domain.model.Degree;
import com.group3.vitamins.employee.domain.model.Employee;
import com.group3.vitamins.employee.domain.model.EmployeeCertificate;
import com.group3.vitamins.employee.domain.model.EmployeeEducation;
import com.group3.vitamins.employee.domain.repository.EmployeeCertificateRepository;
import com.group3.vitamins.employee.domain.repository.EmployeeEducationRepository;
import com.group3.vitamins.employee.domain.repository.EmployeeRepository;
import com.group3.vitamins.employee.contract.EmployeeParticipationUnavailableEvent;
import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.global.infrastructure.config.security.ThrottledPasswordEncoder;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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
    private static final int MAX_SCHOOL_LENGTH = 100; // employee_education.school 컬럼 폭

    // 형식만 거르는 가벼운 검사(RFC 전체 준수 아님) — "local@label.label(.label…)" 최소 형태를 확인한다.
    // ⚠️ 도메인 라벨은 점을 제외한 문자({@code [^@\s.]})로 잡는다 — 라벨 문자에 점이 포함되면 뒤의 `\.` 와
    // 겹쳐 다항 백트래킹(ReDoS, CodeQL High)이 된다. 점을 배제해 각 점이 라벨을 유일하게 나눠 선형이 된다.
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$");

    private final EmployeeAdminPolicy employeeAdminPolicy;
    private final EmployeeRepository employeeRepository;
    private final EmployeeReferenceQueryPort referenceQueryPort;
    private final QualificationReferenceQueryPort qualificationReferenceQueryPort;
    private final EmployeeEducationRepository employeeEducationRepository;
    private final EmployeeCertificateRepository employeeCertificateRepository;
    private final EmployeeRegistrationWriter registrationWriter;
    private final TempPasswordGenerator tempPasswordGenerator;
    private final ThrottledPasswordEncoder passwordEncoder;
    private final InitialPasswordMailPort initialPasswordMailPort;
    private final AccountDeactivationPort accountDeactivationPort;
    private final CompanyCodeQueryPort companyCodeQueryPort;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    public EmployeeRegisterResult register(RegisterEmployeeCommand command) {
        employeeAdminPolicy.assertAdmin(command.actorRole());

        // ── 1 검증 ──
        // 길이 상한은 DB 컬럼 폭과 같다. 여기서 막지 않으면 INSERT 가 데이터 절단으로 터지고, 그 예외가
        // 아래 catch 에서 사번 중복(409)으로 오인 변환된다 — 값 초과를 EMP_INVALID_REQUEST(400)로 먼저 막는다.
        // 회사코드 접두사를 붙여 전역 유일 user_id 를 만든다 (예: "vitas-1234567"). 이후 중복검사·저장은 이 값 기준.
        // company_id 스탬핑에도 같은 회사를 쓰므로 한 번만 읽는다.
        Long companyId = currentCompanyIdProvider.currentCompanyId();
        String userId = prefixWithCompanyCode(required(command.userId()), companyId);
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
        if (!referenceQueryPort.departmentExists(departmentId, companyId)) {
            throw new NotFoundException(EmployeeErrorCode.EMP_DEPARTMENT_NOT_FOUND);
        }
        if (command.jobPositionId() != null && !referenceQueryPort.jobPositionExists(command.jobPositionId(), companyId)) {
            throw new NotFoundException(EmployeeErrorCode.EMP_JOB_POSITION_NOT_FOUND);
        }
        // 학력/자격증은 형식 검증 + 마스터 존재검사까지 여기서 끝낸다 — 해싱(비싼 연산) 전에 실패시키기 위해.
        List<EmployeeEducation> educations = toEducations(command.educations(), userId, companyId);
        List<EmployeeCertificate> certificates = toCertificates(command.certificates(), userId, companyId);

        // ── 2 초기 비밀번호 생성 + 해싱 (트랜잭션 밖) ──
        // 이메일이 없어도 계정에는 비밀번호가 필요하므로 항상 발급한다 — 다만 전달할 수 없어 로그인만 불가하다.
        String rawPassword = tempPasswordGenerator.generate();
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // ── 3 DB 반영 (한 트랜잭션) ──
        Employee employee = Employee.register(
                userId, name, departmentId, command.jobPositionId(), email, phone, hiredAt, companyId);
        try {
            registrationWriter.register(employee, role, encodedPassword, educations, certificates);
        } catch (DataIntegrityViolationException e) {
            // 사전 존재 검사와 INSERT 사이의 레이스로 PK/UNIQUE 가 늦게 터진 경우.
            throw new ConflictException(EmployeeErrorCode.EMP_USER_ID_DUPLICATED, e);
        }

        // ── 4 메일 발송 (커밋 후) ──
        boolean emailRegistered = employee.hasEmail();
        boolean emailSent = false;
        if (emailRegistered) {
            try {
                initialPasswordMailPort.sendInitialPassword(email, userId, name, rawPassword);
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

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        Employee current = employeeRepository.findById(command.userId())
                .orElseThrow(() -> new NotFoundException(EmployeeErrorCode.EMP_NOT_FOUND));
        // 타사 사원은 없는 것으로 취급 — 사번(전역 유일)을 알아도 다른 회사 사원을 수정할 수 없다.
        if (!current.getCompanyId().equals(companyId)) {
            throw new NotFoundException(EmployeeErrorCode.EMP_NOT_FOUND);
        }
        if (current.isSystem()) {
            throw new ForbiddenException(AccountErrorCode.ACC_SYSTEM_ACCOUNT_NOT_ALLOWED);
        }

        // 전달한 필드만 새 값으로, 나머지는 현재값 유지. jobPositionId 는 명시적 null = 직급 미지정.
        // 등록과 동일한 길이·형식 검증을 건다 — 안 하면 DB 컬럼 폭 초과·잘못된 이메일이 UPDATE 절단으로 500 이 된다.
        String name = current.getName();
        if (command.nameProvided()) {
            name = requiredWithMax(command.name(), MAX_NAME_LENGTH); // NOT NULL — 빈 값이면 EMP_INVALID_REQUEST
        }
        String phone = command.phoneProvided()
                ? optionalWithMax(normalize(command.phone()), MAX_PHONE_LENGTH)
                : current.getPhone();
        String email = command.emailProvided()
                ? validateEmail(normalize(command.email()))
                : current.getEmail();

        Long departmentId = current.getDepartmentId();
        if (command.departmentIdProvided()) {
            departmentId = command.departmentId();
            if (departmentId != null && !referenceQueryPort.departmentExists(departmentId, companyId)) {
                throw new NotFoundException(EmployeeErrorCode.EMP_DEPARTMENT_NOT_FOUND);
            }
        }

        Long jobPositionId = current.getJobPositionId();
        if (command.jobPositionIdProvided()) {
            jobPositionId = command.jobPositionId(); // null = 직급 미지정으로 변경
            if (jobPositionId != null && !referenceQueryPort.jobPositionExists(jobPositionId, companyId)) {
                throw new NotFoundException(EmployeeErrorCode.EMP_JOB_POSITION_NOT_FOUND);
            }
        }

        LocalDate hiredAt = current.getHiredAt();
        if (command.hiredAtProvided()) {
            hiredAt = parseRequiredDate(command.hiredAt());
        }

        employeeRepository.updateInfo(current.withInfo(name, phone, email, departmentId, jobPositionId, hiredAt));

        // 학력·자격증은 전체 교체(QUAL-004) — 전달됐을 때만 손댄다(미전송이면 유지). [] 면 삭제만, 값이 있으면 삭제 후 재삽입.
        if (command.educationsProvided()) {
            List<EmployeeEducation> educations = toEducations(command.educations(), command.userId(), companyId);
            employeeEducationRepository.deleteByUserId(command.userId());
            employeeEducationRepository.saveAll(educations);
        }
        if (command.certificatesProvided()) {
            List<EmployeeCertificate> certificates = toCertificates(command.certificates(), command.userId(), companyId);
            employeeCertificateRepository.deleteByUserId(command.userId());
            employeeCertificateRepository.saveAll(certificates);
        }
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
        // 타사 사원은 없는 것으로 취급 — 사번(전역 유일)을 알아도 다른 회사 사원을 퇴사시킬 수 없다.
        if (!current.getCompanyId().equals(currentCompanyIdProvider.currentCompanyId())) {
            throw new NotFoundException(EmployeeErrorCode.EMP_NOT_FOUND);
        }
        if (current.isSystem()) {
            throw new ForbiddenException(AccountErrorCode.ACC_SYSTEM_ACCOUNT_NOT_ALLOWED);
        }
        if (current.isResigned()) {
            throw new ValidationException(EmployeeErrorCode.EMP_ALREADY_RESIGNED);
        }
        LocalDate resignedAt = parseRequiredDate(command.resignedAt());

        employeeRepository.resign(command.userId(), resignedAt);
        accountDeactivationPort.deactivate(command.userId());
        domainEventPublisher.publish(new EmployeeParticipationUnavailableEvent(
                command.userId(), current.getCompanyId()));

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

    /**
     * base 사번 앞에 현재 회사코드를 붙여 전역 유일 {@code user_id} 를 만든다 (예: {@code "vitas-1234567"}).
     * 회사 판별은 접두사가 아니라 {@code company_id} 가 담당하므로, 접두사는 회사간 사번 중복을 피하는 PK 충돌 회피 전용이다.
     * 최종 값이 컬럼 폭({@value #MAX_USER_ID_LENGTH})을 넘으면 base 사번이 너무 긴 것 → {@code EMP_INVALID_REQUEST}(400).
     */
    private String prefixWithCompanyCode(String baseUserId, Long companyId) {
        String companyCode = companyCodeQueryPort.findCodeByCompanyId(companyId);
        if (companyCode == null) {
            throw new IllegalStateException("회사 코드를 찾을 수 없습니다 - companyId=" + companyId);
        }
        String userId = companyCode + "-" + baseUserId;
        if (userId.length() > MAX_USER_ID_LENGTH) {
            throw new ValidationException(EmployeeErrorCode.EMP_INVALID_REQUEST);
        }
        return userId;
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

    /**
     * 학력 목록을 검증·변환한다. 전공 필수·학위 enum·학교 길이를 먼저 보고, 참조 전공이 이 회사 마스터에
     * 모두 있는지 배치로 확인한다(하나라도 없으면 {@code MAJOR_NOT_FOUND}). 비었으면 빈 목록.
     */
    private List<EmployeeEducation> toEducations(List<EducationItem> items, String userId, Long companyId) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<EmployeeEducation> educations = new ArrayList<>(items.size());
        Set<Long> majorIds = new LinkedHashSet<>();
        for (EducationItem item : items) {
            if (item.majorId() == null) {
                throw new ValidationException(EmployeeErrorCode.EMP_INVALID_REQUEST);
            }
            Degree degree = parseDegree(item.degree());
            String school = optionalWithMax(normalize(item.school()), MAX_SCHOOL_LENGTH);
            majorIds.add(item.majorId());
            educations.add(new EmployeeEducation(companyId, userId, item.majorId(), degree, school));
        }
        Set<Long> existing = qualificationReferenceQueryPort.findExistingMajorIds(majorIds, companyId);
        if (!existing.containsAll(majorIds)) {
            throw new NotFoundException(EmployeeErrorCode.MAJOR_NOT_FOUND);
        }
        return educations;
    }

    /**
     * 자격증 목록을 검증·변환한다. 자격증 필수·취득일(선택) 파싱 후, 참조 자격증이 이 회사 마스터에 모두
     * 있는지 배치로 확인한다(하나라도 없으면 {@code CERT_NOT_FOUND}). 비었으면 빈 목록.
     */
    private List<EmployeeCertificate> toCertificates(List<CertificateItem> items, String userId, Long companyId) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<EmployeeCertificate> certificates = new ArrayList<>(items.size());
        Set<Long> certificateIds = new LinkedHashSet<>();
        for (CertificateItem item : items) {
            if (item.certificateId() == null) {
                throw new ValidationException(EmployeeErrorCode.EMP_INVALID_REQUEST);
            }
            LocalDate acquiredDate = parseOptionalDate(item.acquiredDate());
            certificateIds.add(item.certificateId());
            certificates.add(new EmployeeCertificate(companyId, userId, item.certificateId(), acquiredDate));
        }
        Set<Long> existing = qualificationReferenceQueryPort.findExistingCertificateIds(certificateIds, companyId);
        if (!existing.containsAll(certificateIds)) {
            throw new NotFoundException(EmployeeErrorCode.CERT_NOT_FOUND);
        }
        return certificates;
    }

    /** 학위 코드({@code BACHELOR}·{@code MASTER}·{@code DOCTOR})를 enum 으로. 누락·미지원 값은 EMP_INVALID_REQUEST. */
    private Degree parseDegree(String raw) {
        String normalized = normalize(raw);
        if (normalized == null) {
            throw new ValidationException(EmployeeErrorCode.EMP_INVALID_REQUEST);
        }
        try {
            return Degree.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ValidationException(EmployeeErrorCode.EMP_INVALID_REQUEST, e);
        }
    }

    /** 취득일 선택 파싱 — null 은 그대로, 형식이 틀리면 EMP_INVALID_REQUEST. */
    private LocalDate parseOptionalDate(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        try {
            return LocalDate.parse(normalized); // ISO yyyy-MM-dd
        } catch (DateTimeParseException e) {
            throw new ValidationException(EmployeeErrorCode.EMP_INVALID_REQUEST, e);
        }
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
