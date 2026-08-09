package com.group3.vitamins.employee.application.service;

import com.group3.vitamins.account.application.port.MailDeliveryException;
import com.group3.vitamins.account.domain.TempPasswordGenerator;
import com.group3.vitamins.employee.application.command.RegisterBulkCommand;
import com.group3.vitamins.employee.application.command.ValidateBulkCommand;
import com.group3.vitamins.employee.application.policy.EmployeeAdminPolicy;
import com.group3.vitamins.employee.application.port.CompanyCodeQueryPort;
import com.group3.vitamins.employee.application.port.EmployeeBulkReferenceQueryPort;
import com.group3.vitamins.employee.application.port.EmployeeExcelParserPort;
import com.group3.vitamins.employee.application.port.EmployeeExcelTemplatePort;
import com.group3.vitamins.employee.application.port.InitialPasswordMailPort;
import com.group3.vitamins.employee.application.result.BulkAnalysis;
import com.group3.vitamins.employee.application.result.BulkEmployeeRef;
import com.group3.vitamins.employee.application.result.BulkRegisterResult;
import com.group3.vitamins.employee.application.result.BulkRowError;
import com.group3.vitamins.employee.application.result.BulkValidateResult;
import com.group3.vitamins.employee.application.result.BulkValidation;
import com.group3.vitamins.employee.application.result.ParsedEmployeeRow;
import com.group3.vitamins.employee.application.result.ResolvedEmployeeRow;
import com.group3.vitamins.employee.application.usecase.EmployeeBulkUseCase;
import com.group3.vitamins.employee.domain.exception.EmployeeErrorCode;
import com.group3.vitamins.employee.domain.model.Employee;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 사원 엑셀 일괄 등록 서비스 (employee.md §6~§8). 전부 ADMIN 전용 — {@link EmployeeAdminPolicy} 로 판정한다.
 *
 * <p>검증(§7)과 등록(§8)은 {@link #analyze} 로 <b>같은 판정</b>을 공유한다. 검증은 요약만 돌려주고,
 * 등록은 {@code validRows} 를 단건 등록과 동일한 경로(해싱→쓰기→메일)로 행 단위로 태운다(등록은 후속 단계에서 추가).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeBulkService implements EmployeeBulkUseCase {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("xlsx", "xls");
    private static final Set<String> ASSIGNABLE_ROLES = Set.of("MASTER", "MEMBER");
    private static final String ROLE_ADMIN = "ADMIN";

    // DB 컬럼 폭과 동일 (employee: user_id 20 · name 50 · email 100 · phone 20). 초과는 등록 시 절단 500 이 되므로 검증에서 막는다.
    private static final int MAX_USER_ID = 20;
    private static final int MAX_NAME = 50;
    private static final int MAX_EMAIL = 100;
    private static final int MAX_PHONE = 20;

    // 단건 등록과 같은 ReDoS-안전 이메일 패턴 (도메인 라벨에서 점 배제).
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$");

    private final EmployeeAdminPolicy employeeAdminPolicy;
    private final EmployeeExcelTemplatePort excelTemplatePort;
    private final EmployeeExcelParserPort excelParserPort;
    private final EmployeeBulkReferenceQueryPort bulkReferenceQueryPort;
    private final EmployeeRegistrationWriter registrationWriter;
    private final TempPasswordGenerator tempPasswordGenerator;
    private final ThrottledPasswordEncoder passwordEncoder;
    private final InitialPasswordMailPort initialPasswordMailPort;
    private final CompanyCodeQueryPort companyCodeQueryPort;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;

    @Override
    public byte[] getTemplate(String actorRole) {
        employeeAdminPolicy.assertAdmin(actorRole);
        return excelTemplatePort.generate();
    }

    @Override
    @Transactional(readOnly = true)
    public BulkValidateResult validate(ValidateBulkCommand command) {
        employeeAdminPolicy.assertAdmin(command.actorRole());
        return BulkValidateResult.from(
                analyze(command.content(), command.originalFilename(), command.size()));
    }

    /**
     * 일괄 등록 (§8). ⚠️ 이 메서드에 {@code @Transactional} 을 걸지 않는다 — 걸면 전 행이 한 트랜잭션이 돼 부분 등록이 깨진다.
     * 행마다 {@link EmployeeRegistrationWriter} 가 <b>독립 트랜잭션</b>으로 커밋하므로 한 행이 실패해도 나머지는 남는다.
     * 해싱(Argon2 64MB)은 트랜잭션 밖에서 끝내고, 메일은 커밋 후 보낸다(단건 등록 §3 과 동일 경계).
     */
    @Override
    public BulkRegisterResult register(RegisterBulkCommand command) {
        employeeAdminPolicy.assertAdmin(command.actorRole());
        BulkAnalysis analysis = analyze(command.content(), command.originalFilename(), command.size());
        // 이 배치 전체가 같은 회사(로그인 ADMIN) 소속이므로 company_id 스탬핑용 회사번호는 한 번만 읽는다.
        Long companyId = currentCompanyIdProvider.currentCompanyId();

        // skipErrors=false 인데 오류가 있으면 등록하지 않는다(전량 거부). true 면 유효 행만 등록(부분 등록).
        if (!command.skipErrors() && analysis.errorCount() > 0) {
            throw new ValidationException(EmployeeErrorCode.EMP_HAS_ERRORS);
        }

        List<BulkRowError> errors = new ArrayList<>(analysis.errors());
        List<BulkEmployeeRef> emailNotRegistered = new ArrayList<>();
        int registered = 0;
        int emailSent = 0;

        for (ResolvedEmployeeRow row : analysis.validRows()) {
            // 해싱은 트랜잭션 밖 (DB 커넥션을 Argon2 동안 잡지 않는다).
            String rawPassword = tempPasswordGenerator.generate();
            String encodedPassword = passwordEncoder.encode(rawPassword);

            Employee employee = Employee.register(row.userId(), row.name(), row.departmentId(),
                    row.jobPositionId(), row.email(), row.phone(), row.hiredAt(), companyId);
            try {
                registrationWriter.register(employee, row.role(), encodedPassword); // 행별 독립 트랜잭션
            } catch (DataIntegrityViolationException e) {
                // 검증~INSERT 레이스로 사번이 늦게 겹친 경우. 이 행만 실패로 두고 계속 진행한다.
                errors.add(new BulkRowError(row.rowNumber(), row.userId(), row.name(),
                        BulkValidation.USER_ID_DUPLICATED, "이미 등록된 사번입니다"));
                continue;
            }
            registered++;

            // 메일은 커밋 후. 이메일이 없으면 발송하지 않고 emailNotRegistered 로 남긴다(EMP-019).
            if (row.hasEmail()) {
                try {
                    initialPasswordMailPort.sendInitialPassword(row.email(), row.name(), rawPassword);
                    emailSent++;
                } catch (MailDeliveryException e) {
                    // 사원·계정은 이미 만들어졌다. 비밀번호만 다시 보내면 되므로 등록은 성공으로 둔다.
                    log.warn("일괄 등록 초기 비밀번호 메일 발송 실패 - userId={}", row.userId(), e);
                }
            } else {
                emailNotRegistered.add(new BulkEmployeeRef(row.userId(), row.name()));
            }
        }

        int failed = analysis.totalRows() - registered;
        log.info("사원 일괄 등록 - total={} registered={} failed={} emailSent={}",
                analysis.totalRows(), registered, failed, emailSent);
        return new BulkRegisterResult(
                analysis.totalRows(), registered, failed, errors, emailSent, emailNotRegistered);
    }

    // ── 검증·등록 공유 분석 ────────────────────────────────────────────────

    /**
     * 파일 메타(없음·형식·크기)를 먼저 4xx 로 막고, 파싱 후 행별로 검증해 (유효행·오류) 로 나눈다.
     * ⚠️ 파일을 <b>열기 전</b> 알 수 있는 오류만 예외(4xx)다. 연 뒤의 행별 오류는 예외가 아니라 결과의 {@code errors} 로 담는다.
     */
    private BulkAnalysis analyze(byte[] content, String filename, long size) {
        validateFileMeta(content, filename, size);
        List<ParsedEmployeeRow> rows = excelParserPort.parse(content);

        // 파일 내 사번 등장 수 · 최초 등장 행 (중복 판정용)
        Map<String, Long> userIdCounts = rows.stream()
                .map(ParsedEmployeeRow::userId).filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        Map<String, Integer> firstRowByUserId = new java.util.HashMap<>();
        for (ParsedEmployeeRow row : rows) {
            if (row.userId() != null) {
                firstRowByUserId.merge(row.userId(), row.rowNumber(), Math::min);
            }
        }

        // 부서명·직급명·기존 사번 → 배치 해석 (모두 N+1 회피)
        Map<String, Long> deptIds = bulkReferenceQueryPort.resolveDepartmentIdsByName(
                distinct(rows, ParsedEmployeeRow::department));
        Map<String, Long> posIds = bulkReferenceQueryPort.resolveJobPositionIdsByName(
                distinct(rows, ParsedEmployeeRow::jobPosition));
        // 이 등록 배치 전체가 같은 회사(로그인 ADMIN) 소속이므로 회사코드는 한 번만 조회한다.
        // ⚠️ DB 는 이미 접두사 형태(예: "vitas-1234567")로 저장하므로, 기존 사번 존재 검사는 접두사 붙인 값으로 대조한다.
        String companyCode = resolveCompanyCode();
        Set<String> existingUserIds = bulkReferenceQueryPort.findExistingUserIds(
                distinct(rows, ParsedEmployeeRow::userId).stream()
                        .filter(Objects::nonNull)
                        .map(base -> prefixUserId(companyCode, base))
                        .collect(Collectors.toSet()));

        List<ResolvedEmployeeRow> validRows = new ArrayList<>();
        List<BulkRowError> errors = new ArrayList<>();
        for (ParsedEmployeeRow row : rows) {
            BulkRowError error = validateRow(row, userIdCounts, firstRowByUserId, deptIds, existingUserIds, companyCode);
            if (error != null) {
                errors.add(error);
            } else {
                validRows.add(toResolved(row, deptIds, posIds, companyCode));
            }
        }

        int emailNotRegistered = (int) validRows.stream().filter(r -> !r.hasEmail()).count();
        return new BulkAnalysis(rows.size(), validRows, errors, emailNotRegistered);
    }

    /** 행 하나를 우선순위대로 검증해 첫 오류를 돌려준다(없으면 null = 유효). */
    private BulkRowError validateRow(ParsedEmployeeRow row, Map<String, Long> counts,
                                     Map<String, Integer> firstRow, Map<String, Long> deptIds,
                                     Set<String> existingUserIds, String companyCode) {
        // 1) 필수값·길이·형식 (REQUIRED_COLUMN 버킷)
        if (row.userId() == null) {
            return err(row, BulkValidation.REQUIRED_COLUMN, "필수 컬럼 누락: 사번");
        }
        // 접두사(회사코드-)까지 붙인 최종 user_id 가 컬럼 폭을 넘으면 안 된다 — base 사번이 너무 긴 것.
        if (prefixUserId(companyCode, row.userId()).length() > MAX_USER_ID) {
            return err(row, BulkValidation.REQUIRED_COLUMN, "사번이 회사코드 포함 " + MAX_USER_ID + "자를 초과했습니다");
        }
        if (row.name() == null) {
            return err(row, BulkValidation.REQUIRED_COLUMN, "필수 컬럼 누락: 이름");
        }
        if (row.name().length() > MAX_NAME) {
            return err(row, BulkValidation.REQUIRED_COLUMN, "이름이 " + MAX_NAME + "자를 초과했습니다");
        }
        if (row.department() == null) {
            return err(row, BulkValidation.REQUIRED_COLUMN, "필수 컬럼 누락: 부서명");
        }
        if (row.hiredAt() == null) {
            return err(row, BulkValidation.REQUIRED_COLUMN, "필수 컬럼 누락: 입사일");
        }
        if (parseDate(row.hiredAt()) == null) {
            return err(row, BulkValidation.REQUIRED_COLUMN, "입사일 형식 오류 (yyyy-MM-dd)");
        }
        if (row.role() == null) {
            return err(row, BulkValidation.REQUIRED_COLUMN, "필수 컬럼 누락: 권한");
        }

        // 2) 권한 — ADMIN 은 전용 코드, 그 외 허용값 아니면 형식 오류
        if (ROLE_ADMIN.equalsIgnoreCase(row.role())) {
            return err(row, BulkValidation.ADMIN_ROLE_NOT_ALLOWED, "엑셀로는 관리자 권한을 부여할 수 없습니다");
        }
        if (!ASSIGNABLE_ROLES.contains(row.role().toUpperCase(Locale.ROOT))) {
            return err(row, BulkValidation.REQUIRED_COLUMN, "권한 값이 올바르지 않습니다 (MASTER/MEMBER)");
        }

        // 3) 선택값 형식 — 이메일·연락처 (있을 때만)
        if (row.email() != null && (row.email().length() > MAX_EMAIL || !EMAIL_PATTERN.matcher(row.email()).matches())) {
            return err(row, BulkValidation.REQUIRED_COLUMN, "이메일 형식 오류");
        }
        if (row.phone() != null && row.phone().length() > MAX_PHONE) {
            return err(row, BulkValidation.REQUIRED_COLUMN, "연락처가 " + MAX_PHONE + "자를 초과했습니다");
        }

        // 4) 사번 중복 — 파일 내 우선, 그다음 DB 기존
        if (counts.getOrDefault(row.userId(), 0L) > 1) {
            return err(row, BulkValidation.USER_ID_DUPLICATED,
                    "파일 내 사번 중복 (" + firstRow.get(row.userId()) + "행)");
        }
        if (existingUserIds.contains(prefixUserId(companyCode, row.userId()))) {
            return err(row, BulkValidation.USER_ID_DUPLICATED, "이미 등록된 사번입니다");
        }

        // 5) 부서 존재 — 형제 유니크라 유일 매칭만 통과(모호·부재 모두 NOT_FOUND)
        if (!deptIds.containsKey(row.department())) {
            return err(row, BulkValidation.DEPARTMENT_NOT_FOUND, "부서를 찾을 수 없습니다: " + row.department());
        }

        return null; // 유효
    }

    /** 유효 행 → 등록 가능한 값으로 변환. 사번은 접두사를 붙인 최종 user_id 로 굳힌다. 직급명은 불일치·미지정이면 null(오류 아님). */
    private ResolvedEmployeeRow toResolved(ParsedEmployeeRow row, Map<String, Long> deptIds,
                                           Map<String, Long> posIds, String companyCode) {
        Long jobPositionId = row.jobPosition() == null ? null : posIds.get(row.jobPosition());
        return new ResolvedEmployeeRow(
                row.rowNumber(), prefixUserId(companyCode, row.userId()), row.name(),
                deptIds.get(row.department()), jobPositionId, parseDate(row.hiredAt()),
                row.email(), row.phone(), row.role().toUpperCase(Locale.ROOT));
    }

    /** 현재 로그인 회사의 코드를 조회한다 (접두사 재료). 등록 배치 전체가 같은 회사라 한 번만 부른다. */
    private String resolveCompanyCode() {
        Long companyId = currentCompanyIdProvider.currentCompanyId();
        String companyCode = companyCodeQueryPort.findCodeByCompanyId(companyId);
        if (companyCode == null) {
            throw new IllegalStateException("회사 코드를 찾을 수 없습니다 - companyId=" + companyId);
        }
        return companyCode;
    }

    /** base 사번 앞에 회사코드를 붙여 전역 유일 user_id 를 만든다 (예: {@code "vitas-1234567"}). 회사 판별은 company_id 담당. */
    private String prefixUserId(String companyCode, String baseUserId) {
        return companyCode + "-" + baseUserId;
    }

    private void validateFileMeta(byte[] content, String filename, long size) {
        if (content == null || content.length == 0) {
            throw new ValidationException(EmployeeErrorCode.EMP_FILE_REQUIRED);
        }
        if (!hasAllowedExtension(filename)) {
            throw new ValidationException(EmployeeErrorCode.EMP_FILE_TYPE_INVALID);
        }
        // size(신고값)와 실제 바이트 길이를 함께 본다 — size 를 속이거나 컨트롤러가 20MB 까지 읽어온 뒤라도
        // 5MB 초과 본문은 여기서 막는다.
        if (size > MAX_FILE_SIZE || content.length > MAX_FILE_SIZE) {
            throw new ValidationException(EmployeeErrorCode.EMP_FILE_SIZE_EXCEEDED);
        }
    }

    private boolean hasAllowedExtension(String filename) {
        if (filename == null) {
            return false;
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0) {
            return false;
        }
        return ALLOWED_EXTENSIONS.contains(filename.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    private Set<String> distinct(List<ParsedEmployeeRow> rows, Function<ParsedEmployeeRow, String> field) {
        return rows.stream().map(field).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value); // ISO yyyy-MM-dd
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private BulkRowError err(ParsedEmployeeRow row, BulkValidation validation, String message) {
        return new BulkRowError(row.rowNumber(), row.userId(), row.name(), validation, message);
    }
}
