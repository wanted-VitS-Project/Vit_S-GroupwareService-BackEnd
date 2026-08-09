package com.group3.vitamins.department.application.service;

import com.group3.vitamins.department.application.command.CreateDepartmentCommand;
import com.group3.vitamins.department.application.command.DeleteDepartmentCommand;
import com.group3.vitamins.department.application.command.RenameDepartmentCommand;
import com.group3.vitamins.department.application.policy.DepartmentAdminPolicy;
import com.group3.vitamins.department.application.port.DepartmentEmployeeQueryPort;
import com.group3.vitamins.department.application.result.DepartmentResult;
import com.group3.vitamins.department.application.usecase.DepartmentCommandUseCase;
import com.group3.vitamins.department.domain.exception.DepartmentErrorCode;
import com.group3.vitamins.department.domain.model.Department;
import com.group3.vitamins.department.domain.repository.DepartmentRepository;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.global.infrastructure.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 부서 생성·수정·삭제 유스케이스 (`.ai/api/department.md` §2·§3·§4).
 *
 * <p>세 API 모두 <b>ADMIN 전용</b>이다. ADMIN 판정은 {@link DepartmentAdminPolicy} 가 도메인 코드
 * ({@code ACC_ADMIN_REQUIRED})와 함께 한다 — 명세가 일반 403 이 아니라 그 코드를 요구하기 때문이다.
 *
 * <p>쓰기·단건 조회는 {@link DepartmentRepository}(JPA), 사원 집계처럼 {@code employee} 를 가로지르는
 * 조회는 {@link DepartmentEmployeeQueryPort}(MyBatis) 다. 조회(트리 조립)는 {@code DepartmentQueryService} 가 맡는다.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DepartmentCommandService implements DepartmentCommandUseCase {

    /** 부서명 최대 길이 (`.ai/api/department.md` §2·§3) */
    private static final int MAX_NAME_LENGTH = 50;

    private final DepartmentRepository departmentRepository;
    private final DepartmentEmployeeQueryPort departmentEmployeeQueryPort;
    private final DepartmentAdminPolicy departmentAdminPolicy;

    /**
     * 부서 생성 (`.ai/api/department.md` §2).
     *
     * <p>{@code parentId} 유무로 최상위/하위가 갈린다. 하위 부서를 상위로 지정하면 계층이 3단이 되므로
     * {@code DEPT_MAX_DEPTH_EXCEEDED}(409) 로 막는다. 부서명은 <b>같은 상위 부서 안에서만</b> 유니크하다
     * (2026-08-06). DB 복합 유니크 {@code uk_department_parent_name(parent_key, name)} 이 자식·최상위를
     * 모두 막고(최상위는 {@code parent_key=COALESCE(parent_id,0)=0} 공유), 아래 app 검사는 친절한 선처리다.
     */
    @Override
    public DepartmentResult create(CreateDepartmentCommand command) {
        departmentAdminPolicy.assertAdmin(command.role());
        validateName(command.name());

        String parentName = null;
        Long parentId = command.parentId();
        if (parentId != null) {
            // 부모 행을 배타 잠금으로 읽는다 — 삭제(findByIdForUpdate)와 잠금 순서를 맞춰,
            // "부모가 동시에 삭제돼 저장 시 FK 위반(→ 이름중복으로 오분류)" 레이스를 원천 차단한다.
            // 잠금을 쥐고 있는 동안 부모는 삭제되지 못하므로, 커밋 시점까지 parentId 참조가 유효하다.
            Department parent = departmentRepository.findByIdForUpdate(parentId)
                    .orElseThrow(() -> new NotFoundException(DepartmentErrorCode.DEPT_PARENT_NOT_FOUND));
            if (!parent.isRoot()) {
                throw new ConflictException(DepartmentErrorCode.DEPT_MAX_DEPTH_EXCEEDED);
            }
            parentName = parent.getName();
        }
        if (departmentRepository.existsSiblingName(command.name(), parentId)) {
            throw new ConflictException(DepartmentErrorCode.DEPT_NAME_DUPLICATED);
        }

        // 검사 통과 후 저장까지의 틈에 같은 이름이 먼저 커밋될 수 있다. uk_department_parent_name(parent_key, name)
        // 이 최종 방어선이라 그 위반을 500 이 아니라 명세의 409(DEPT_NAME_DUPLICATED)로 돌려준다.
        // parent_key = COALESCE(parent_id, 0) 라 최상위(0 공유)·자식 모두 DB 가 막는다(위 app 검사는 친절한 선처리).
        // 부모 행을 위에서 잠갔으므로 이 시점의 제약 위반은 부서명 유니크뿐이다(FK 위반 불가) → 매핑이 결정적.
        // save 구현이 saveAndFlush 라 위반을 즉시 감지한다.
        Department saved;
        try {
            saved = departmentRepository.save(
                    Department.create(command.name(), parentId, TenantContext.currentCompanyId()));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(DepartmentErrorCode.DEPT_NAME_DUPLICATED, e);
        }
        log.info("부서 생성 — departmentId={} name={} parentId={}", saved.getDepartmentId(), command.name(), parentId);
        return new DepartmentResult(
                saved.getDepartmentId(), saved.getName(), parentId, parentName, 0, 0);
    }

    /**
     * 부서명 수정 (`.ai/api/department.md` §3).
     *
     * <p>⛔ 상위 부서는 바꾸지 않는다 — 부서 이동 기능이 없다. 이름만 바꾼다.
     * 유니크 검증은 자기 자신을 제외해, 이름을 그대로 다시 저장해도 중복으로 튕기지 않게 한다.
     */
    @Override
    public DepartmentResult rename(RenameDepartmentCommand command) {
        departmentAdminPolicy.assertAdmin(command.role());
        validateName(command.name());

        Department department = departmentRepository.findById(command.departmentId())
                .orElseThrow(() -> new NotFoundException(DepartmentErrorCode.DEPT_NOT_FOUND));
        // 상위는 바뀌지 않으므로 그 부서의 현재 parentId 기준 형제끼리, 자기 자신은 제외하고 비교한다.
        if (departmentRepository.existsSiblingNameExcludingSelf(
                command.name(), department.getParentId(), command.departmentId())) {
            throw new ConflictException(DepartmentErrorCode.DEPT_NAME_DUPLICATED);
        }

        department.rename(command.name());
        // 검사~커밋 틈의 동시 중복(하위 부서)은 uk_department_parent_name 이 잡는다 → 500 대신 409 (saveAndFlush 라 즉시 감지).
        Department saved;
        try {
            saved = departmentRepository.save(department);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(DepartmentErrorCode.DEPT_NAME_DUPLICATED, e);
        }
        log.info("부서명 수정 — departmentId={} name={}", saved.getDepartmentId(), saved.getName());
        return new DepartmentResult(
                saved.getDepartmentId(), saved.getName(),
                saved.getParentId(), resolveParentName(saved.getParentId()), 0, 0);
    }

    /**
     * 부서 삭제 (`.ai/api/department.md` §4).
     *
     * <p>차단 조건이 둘이다 — 직속 사원이 있거나 하위 부서가 있으면 지우지 않는다. 하위 부서를
     * CASCADE 로 함께 지우면 팀 하나 지우려다 본부 전체가 사라진다. 소프트 삭제가 아니라 행을 제거한다.
     * 프론트가 "왜 막혔는지" 를 보여줄 수 있게 {@code message} 에 개수를 담는다.
     */
    @Override
    public void delete(DeleteDepartmentCommand command) {
        departmentAdminPolicy.assertAdmin(command.role());

        // 부서 행을 배타 잠금으로 읽는다 — 아래 차단 검사와 삭제 사이에 다른 트랜잭션이 이 부서로
        // 사원 배정·하위 부서 생성을 끼워 넣어 FK 위반(500)이 나는 레이스를 막는다.
        Department department = departmentRepository.findByIdForUpdate(command.departmentId())
                .orElseThrow(() -> new NotFoundException(DepartmentErrorCode.DEPT_NOT_FOUND));

        long directEmployees = departmentEmployeeQueryPort.countDirectEmployees(command.departmentId());
        if (directEmployees > 0) {
            throw new ConflictException(DepartmentErrorCode.DEPT_HAS_EMPLOYEES,
                    "소속 사원 " + directEmployees + "명이 있어 삭제할 수 없습니다.");
        }
        long children = departmentRepository.countByParentId(command.departmentId());
        if (children > 0) {
            throw new ConflictException(DepartmentErrorCode.DEPT_HAS_CHILDREN,
                    "하위 부서 " + children + "개가 있어 삭제할 수 없습니다.");
        }

        departmentRepository.delete(department);
        log.info("부서 삭제 — departmentId={}", command.departmentId());
    }

    // ===== 공통 =====

    /** 부서명은 비어 있거나 50자를 넘을 수 없다 (`.ai/api/department.md` §2·§3). */
    private void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > MAX_NAME_LENGTH) {
            throw new ValidationException(DepartmentErrorCode.DEPT_INVALID_REQUEST);
        }
    }

    /** 상위 부서명을 조회한다. 최상위 부서면 {@code null}. FK 로 부모 존재가 보장되지만 방어적으로 둔다. */
    private String resolveParentName(Long parentId) {
        if (parentId == null) {
            return null;
        }
        return departmentRepository.findById(parentId)
                .map(Department::getName)
                .orElse(null);
    }
}
