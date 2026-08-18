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
import com.group3.vitamins.employee.application.port.QualificationMasterCreatePort;
import com.group3.vitamins.employee.application.result.BulkAnalysis;
import com.group3.vitamins.employee.application.result.BulkEmployeeRef;
import com.group3.vitamins.employee.application.result.BulkRegisterResult;
import com.group3.vitamins.employee.application.result.BulkRowError;
import com.group3.vitamins.employee.application.result.BulkValidateResult;
import com.group3.vitamins.employee.application.result.BulkValidation;
import com.group3.vitamins.employee.application.result.ParsedEmployeeRow;
import com.group3.vitamins.employee.application.result.PendingMaster;
import com.group3.vitamins.employee.application.result.PendingMasters;
import com.group3.vitamins.employee.application.result.ResolvedEmployeeRow;
import com.group3.vitamins.employee.application.result.RowCertificate;
import com.group3.vitamins.employee.application.result.RowEducation;
import com.group3.vitamins.employee.application.usecase.EmployeeBulkUseCase;
import com.group3.vitamins.employee.domain.exception.EmployeeErrorCode;
import com.group3.vitamins.employee.domain.model.Degree;
import com.group3.vitamins.employee.domain.model.Employee;
import com.group3.vitamins.employee.domain.model.EmployeeCertificate;
import com.group3.vitamins.employee.domain.model.EmployeeEducation;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.global.infrastructure.config.security.ThrottledPasswordEncoder;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.qualification.domain.model.QualificationNameRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
 * 등록은 {@code validRows} 를 단건 등록과 동일한 경로(해싱→쓰기→메일)로 행 단위로 태운다.
 *
 * <p>{@code autoCreateMasters}(2026-08-18) — 목록에 없는 전공/자격증을 오류 대신 <b>자동 생성 대상</b>으로 분류하고(검증 {@code newMasters}),
 * 등록은 사원 행을 쓰기 <b>전에</b> {@link QualificationMasterCreatePort} 로 마스터를 만든 뒤 참조한다(등록 {@code createdMasters}).
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
    private final QualificationMasterCreatePort qualificationMasterCreatePort;

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
                analyze(command.content(), command.originalFilename(), command.size(), command.autoCreateMasters()));
    }

    /**
     * 일괄 등록 (§8). ⚠️ 이 메서드에 {@code @Transactional} 을 걸지 않는다 — 걸면 전 행이 한 트랜잭션이 돼 부분 등록이 깨진다.
     * 행마다 {@link EmployeeRegistrationWriter} 가 <b>독립 트랜잭션</b>으로 커밋하므로 한 행이 실패해도 나머지는 남는다.
     * 해싱(Argon2 64MB)은 트랜잭션 밖에서 끝내고, 메일은 커밋 후 보낸다(단건 등록 §3 과 동일 경계).
     *
     * <p>{@code autoCreateMasters=true} 면 사원 행을 쓰기 <b>전에</b> 자동 생성 대상 마스터를 먼저 만든다 — 순서가 뒤집히면 FK 가 없다.
     * {@code EMP_HAS_ERRORS} 판정이 그보다 앞이라 전량 거부 때는 마스터도 생기지 않는다.
     */
    @Override
    public BulkRegisterResult register(RegisterBulkCommand command) {
        employeeAdminPolicy.assertAdmin(command.actorRole());
        BulkAnalysis analysis = analyze(command.content(), command.originalFilename(), command.size(),
                command.autoCreateMasters());
        // 이 배치 전체가 같은 회사(로그인 ADMIN) 소속이므로 company_id 스탬핑용 회사번호는 한 번만 읽는다.
        Long companyId = currentCompanyIdProvider.currentCompanyId();

        // skipErrors=false 인데 오류가 있으면 등록하지 않는다(전량 거부). true 면 유효 행만 등록(부분 등록).
        if (!command.skipErrors() && analysis.errorCount() > 0) {
            throw new ValidationException(EmployeeErrorCode.EMP_HAS_ERRORS);
        }

        // 자동 생성 대상 마스터를 사원보다 먼저 만든다(이름 → id). autoCreateMasters=false 면 newMasters 가 비어 있어 아무것도 안 만든다.
        Map<String, Long> createdMajorIds = createMasters(analysis.newMasters().majors(),
                names -> qualificationMasterCreatePort.createMajors(names, companyId, command.actorRole()));
        Map<String, Long> createdCertIds = createMasters(analysis.newMasters().certificates(),
                names -> qualificationMasterCreatePort.createCertificates(names, companyId, command.actorRole()));

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
            // 학력·자격증은 analyze 에서 해석한 마스터 ID(자동 생성분은 방금 만든 id)·학위로 도메인 객체를 굳힌다(학교·취득일은 엑셀에 없음 → null).
            List<EmployeeEducation> educations = row.educations().stream()
                    .map(e -> new EmployeeEducation(companyId, row.userId(),
                            e.majorId() != null ? e.majorId() : requireMasterId(createdMajorIds, e.majorName(), "전공"),
                            e.degree(), null))
                    .toList();
            List<EmployeeCertificate> certificates = row.certificates().stream()
                    .map(c -> new EmployeeCertificate(companyId, row.userId(),
                            c.certificateId() != null ? c.certificateId()
                                    : requireMasterId(createdCertIds, c.certificateName(), "자격증"), null))
                    .toList();
            try {
                // 사원과 한 트랜잭션으로 저장한다.
                registrationWriter.register(employee, row.role(), encodedPassword,
                        educations, certificates); // 행별 독립 트랜잭션
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
                    initialPasswordMailPort.sendInitialPassword(row.email(), row.userId(), row.name(), rawPassword);
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
                analysis.totalRows(), registered, failed, errors, emailSent, emailNotRegistered, analysis.newMasters());
    }

    /** 자동 생성 대상이 있을 때만 포트를 부른다(빈 목록이면 호출 자체를 안 한다). */
    private Map<String, Long> createMasters(List<PendingMaster> pending, Function<List<String>, Map<String, Long>> creator) {
        if (pending.isEmpty()) {
            return Map.of();
        }
        return creator.apply(pending.stream().map(PendingMaster::name).toList());
    }

    /**
     * 자동 생성 마스터의 이름→ID 매핑을 꺼낸다. 포트 계약({@link QualificationMasterCreatePort} — "요청한 모든 이름을 키로 갖는다")이
     * 깨져 매핑이 비면 {@code null} FK 로 새지 말고 즉시 끊는다 — 안 그러면 그 행이 INSERT 에서 무결성 위반으로 실패하고
     * {@code DataIntegrityViolationException} 이 사번 중복으로 <b>오분류</b>돼 관리자가 원인을 잘못 짚는다.
     * 마스터는 사원과 독립 생명주기라 여기서 중단해도 데이터 손실이 아니다.
     */
    private Long requireMasterId(Map<String, Long> createdIds, String name, String kind) {
        Long id = createdIds.get(name);
        if (id == null) {
            throw new IllegalStateException(kind + " 마스터 자동 생성 ID 누락 - name=" + name);
        }
        return id;
    }

    // ── 검증·등록 공유 분석 ────────────────────────────────────────────────

    /**
     * 파일 메타(없음·형식·크기)를 먼저 4xx 로 막고, 파싱 후 행별로 검증해 (유효행·오류) 로 나눈다.
     * ⚠️ 파일을 <b>열기 전</b> 알 수 있는 오류만 예외(4xx)다. 연 뒤의 행별 오류는 예외가 아니라 결과의 {@code errors} 로 담는다.
     */
    private BulkAnalysis analyze(byte[] content, String filename, long size, boolean autoCreateMasters) {
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

        // 부서명·직급명·기존 사번 → 배치 해석 (모두 N+1 회피). 이름 해석은 현재 회사 범위로만 매칭한다(타사 부서·직급 배정 차단).
        Long companyId = currentCompanyIdProvider.currentCompanyId();
        Map<String, Long> deptIds = bulkReferenceQueryPort.resolveDepartmentIdsByName(
                distinct(rows, ParsedEmployeeRow::department), companyId);
        Map<String, Long> posIds = bulkReferenceQueryPort.resolveJobPositionIdsByName(
                distinct(rows, ParsedEmployeeRow::jobPosition), companyId);
        // 이 등록 배치 전체가 같은 회사(로그인 ADMIN) 소속이므로 회사코드는 한 번만 조회한다.
        // ⚠️ DB 는 이미 접두사 형태(예: "vitas-1234567")로 저장하므로, 기존 사번 존재 검사는 접두사 붙인 값으로 대조한다.
        String companyCode = resolveCompanyCode();
        Set<String> existingUserIds = bulkReferenceQueryPort.findExistingUserIds(
                distinct(rows, ParsedEmployeeRow::userId).stream()
                        .filter(Objects::nonNull)
                        .map(base -> prefixUserId(companyCode, base))
                        .collect(Collectors.toSet()));
        // 전공명·자격증명도 배치로 해석한다(N+1 회피). 파일 전체 셀에서 이름을 뽑아 회사 범위로 한 번에 조회.
        Map<String, Long> majorIds = bulkReferenceQueryPort.resolveMajorIdsByName(majorNamesIn(rows), companyId);
        Map<String, Long> certIds = bulkReferenceQueryPort.resolveCertificateIdsByName(certificateNamesIn(rows), companyId);

        List<ResolvedEmployeeRow> validRows = new ArrayList<>();
        List<BulkRowError> errors = new ArrayList<>();
        for (ParsedEmployeeRow row : rows) {
            BulkRowError error = validateRow(row, userIdCounts, firstRowByUserId, deptIds, existingUserIds,
                    companyCode, majorIds, certIds, autoCreateMasters);
            if (error != null) {
                errors.add(error);
            } else {
                validRows.add(toResolved(row, deptIds, posIds, companyCode, majorIds, certIds));
            }
        }

        int emailNotRegistered = (int) validRows.stream().filter(r -> !r.hasEmail()).count();
        // 자동 생성 대상은 유효 행 기준 — 다른 오류로 빠지는 행만 참조하는 이름은 만들지 않는다. autoCreate=false 면 id 없는 항목이 없어 자연히 빈 값.
        return new BulkAnalysis(rows.size(), validRows, errors, emailNotRegistered, collectNewMasters(validRows));
    }

    /**
     * 유효 행에서 마스터 ID 가 없는(=자동 생성 대상) 전공/자격증 이름을 <b>첫 등장 순</b>으로 모으고, 이름별로 참조하는 행 수를 센다.
     * 한 행이 같은 이름을 두 번 써도 행 수는 1 이다(사람이 "몇 명에게 붙는지" 보는 값).
     */
    private PendingMasters collectNewMasters(List<ResolvedEmployeeRow> validRows) {
        Map<String, Integer> majors = new LinkedHashMap<>();
        Map<String, Integer> certs = new LinkedHashMap<>();
        for (ResolvedEmployeeRow row : validRows) {
            Set<String> rowMajors = new LinkedHashSet<>();
            for (RowEducation e : row.educations()) {
                if (e.majorId() == null) {
                    rowMajors.add(e.majorName());
                }
            }
            rowMajors.forEach(n -> majors.merge(n, 1, Integer::sum));
            Set<String> rowCerts = new LinkedHashSet<>();
            for (RowCertificate c : row.certificates()) {
                if (c.certificateId() == null) {
                    rowCerts.add(c.certificateName());
                }
            }
            rowCerts.forEach(n -> certs.merge(n, 1, Integer::sum));
        }
        if (majors.isEmpty() && certs.isEmpty()) {
            return PendingMasters.empty();
        }
        return new PendingMasters(toPending(majors), toPending(certs));
    }

    private List<PendingMaster> toPending(Map<String, Integer> counts) {
        return counts.entrySet().stream().map(e -> new PendingMaster(e.getKey(), e.getValue())).toList();
    }

    /** 행 하나를 우선순위대로 검증해 첫 오류를 돌려준다(없으면 null = 유효). */
    private BulkRowError validateRow(ParsedEmployeeRow row, Map<String, Long> counts,
                                     Map<String, Integer> firstRow, Map<String, Long> deptIds,
                                     Set<String> existingUserIds, String companyCode,
                                     Map<String, Long> majorIds, Map<String, Long> certIds,
                                     boolean autoCreateMasters) {
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

        // 6) 학력 — "전공:학위" 형식·학위 표기(학사/석사/박사)·전공 마스터 존재
        //    autoCreateMasters 면 "없음"은 오류가 아니라 생성 대상이다. 단 만들 수 있는 이름이어야 한다(마스터 이름 규칙 — 100자·금지 문자).
        for (EducationSegment seg : parseEducationSegments(row.education())) {
            if (seg.majorName() == null || seg.degreeLabel() == null) {
                return err(row, BulkValidation.REQUIRED_COLUMN, "학력 형식 오류 (전공:학위)");
            }
            if (Degree.fromKoreanLabel(seg.degreeLabel()) == null) {
                return err(row, BulkValidation.REQUIRED_COLUMN, "학위 표기 오류 (학사/석사/박사): " + seg.degreeLabel());
            }
            if (!majorIds.containsKey(seg.majorName())) {
                if (!autoCreateMasters) {
                    return err(row, BulkValidation.EDU_NOT_FOUND, "전공을 찾을 수 없습니다: " + seg.majorName());
                }
                if (!QualificationNameRule.isValid(seg.majorName())) {
                    return err(row, BulkValidation.REQUIRED_COLUMN,
                            "전공명 형식 오류 (" + QualificationNameRule.MAX_LENGTH + "자 이내, 금지 문자 없음): " + seg.majorName());
                }
            }
        }

        // 7) 자격증 — 자격증 마스터 존재 (autoCreateMasters 면 위와 같이 생성 대상 + 이름 규칙)
        for (String certName : parseCertificateNames(row.certificate())) {
            if (!certIds.containsKey(certName)) {
                if (!autoCreateMasters) {
                    return err(row, BulkValidation.CERT_NOT_FOUND, "자격증을 찾을 수 없습니다: " + certName);
                }
                if (!QualificationNameRule.isValid(certName)) {
                    return err(row, BulkValidation.REQUIRED_COLUMN,
                            "자격증명 형식 오류 (" + QualificationNameRule.MAX_LENGTH + "자 이내, ':' 등 금지 문자 없음): " + certName);
                }
            }
        }

        return null; // 유효
    }

    /**
     * 유효 행 → 등록 가능한 값으로 변환. 사번은 접두사를 붙인 최종 user_id 로 굳힌다. 직급명은 불일치·미지정이면 null(오류 아님).
     * 학력·자격증은 이름+마스터 ID 로 둔다 — ID 가 null 인 항목은 자동 생성 대상(검증에서 통과시킨 경우뿐)이라 등록 단계가 채운다.
     */
    private ResolvedEmployeeRow toResolved(ParsedEmployeeRow row, Map<String, Long> deptIds,
                                           Map<String, Long> posIds, String companyCode,
                                           Map<String, Long> majorIds, Map<String, Long> certIds) {
        Long jobPositionId = row.jobPosition() == null ? null : posIds.get(row.jobPosition());
        String userId = prefixUserId(companyCode, row.userId());

        List<RowEducation> educations = parseEducationSegments(row.education()).stream()
                .map(seg -> new RowEducation(seg.majorName(), majorIds.get(seg.majorName()),
                        Degree.fromKoreanLabel(seg.degreeLabel())))
                .toList();
        List<RowCertificate> certificates = parseCertificateNames(row.certificate()).stream()
                .map(name -> new RowCertificate(name, certIds.get(name)))
                .toList();

        return new ResolvedEmployeeRow(
                row.rowNumber(), userId, row.name(),
                deptIds.get(row.department()), jobPositionId, parseDate(row.hiredAt()),
                row.email(), row.phone(), row.role().toUpperCase(Locale.ROOT),
                educations, certificates);
    }

    // ── 학력/자격증 셀 파싱 (employee.md §6 · HR-V1 QUAL-005) ──────────────

    /** 파일 전체 학력 셀에서 전공명을 뽑는다(배치 마스터 조회용). 형식 불량 세그먼트는 majorName 이 null 이라 자연히 빠진다. */
    private Set<String> majorNamesIn(List<ParsedEmployeeRow> rows) {
        return rows.stream()
                .flatMap(r -> parseEducationSegments(r.education()).stream())
                .map(EducationSegment::majorName).filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /** 파일 전체 자격증 셀에서 자격증명을 뽑는다(배치 마스터 조회용). */
    private Set<String> certificateNamesIn(List<ParsedEmployeeRow> rows) {
        return rows.stream()
                .flatMap(r -> parseCertificateNames(r.certificate()).stream())
                .collect(Collectors.toSet());
    }

    /**
     * 항목 구분자 — 세미콜론(;)·쉼표(,)·셀 내 줄바꿈(Alt+Enter=\n, \r 포함). 연속 구분자는 하나로 본다.
     * 각 항목은 뒤에서 {@code trim()} 하므로 "구분자 뒤 공백"({@code 컴퓨터공학:학사, 소프트웨어공학:석사})도 흡수된다.
     * ⚠️ 구분자가 든 마스터 이름은 쪼개진다 — 그래서 마스터 이름 규칙({@link QualificationNameRule})이 `,` `;` `:` 줄바꿈을 금지한다(2026-08-18).
     */
    private static final String ITEM_SEPARATORS = "[;,\\r\\n]+";

    /** 학력 셀 "전공:학위" 여러 개 → 세그먼트 목록. 빈 세그먼트는 건너뛰고, 형식 불량(콜론 없음·빈 값)은 null 필드로 표시한다. */
    private List<EducationSegment> parseEducationSegments(String cell) {
        if (cell == null) {
            return List.of();
        }
        List<EducationSegment> segments = new ArrayList<>();
        for (String part : cell.split(ITEM_SEPARATORS)) {
            String seg = part.trim();
            if (seg.isEmpty()) {
                continue;
            }
            int colon = seg.indexOf(':');
            if (colon < 0) {
                segments.add(new EducationSegment(null, null)); // 콜론 없음 = 형식 불량
                continue;
            }
            String major = seg.substring(0, colon).trim();
            String degree = seg.substring(colon + 1).trim();
            segments.add(new EducationSegment(major.isEmpty() ? null : major, degree.isEmpty() ? null : degree));
        }
        return segments;
    }

    /** 자격증 셀 "자격증명" 여러 개 → 이름 목록. 구분자는 {@link #ITEM_SEPARATORS}. 빈 항목은 건너뛴다. */
    private List<String> parseCertificateNames(String cell) {
        if (cell == null) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (String part : cell.split(ITEM_SEPARATORS)) {
            String name = part.trim();
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        return names;
    }

    /** 학력 세그먼트 원시값(전공명·학위표기). 둘 중 하나라도 null 이면 형식 불량이다. */
    private record EducationSegment(String majorName, String degreeLabel) {
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
