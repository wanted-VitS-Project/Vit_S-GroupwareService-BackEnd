package com.group3.vitamins.department.application;

import com.group3.vitamins.account.domain.exception.AccountErrorCode;
import com.group3.vitamins.department.domain.exception.DepartmentErrorCode;
import com.group3.vitamins.department.infrastructure.persistence.DepartmentEntity;
import com.group3.vitamins.department.infrastructure.persistence.DepartmentJpaRepository;
import com.group3.vitamins.department.infrastructure.persistence.mapper.DepartmentMapper;
import com.group3.vitamins.department.presentation.api.dto.response.DepartmentCreateResponse;
import com.group3.vitamins.department.presentation.api.dto.response.DepartmentUpdateResponse;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 부서 생성·수정·삭제 유스케이스 (`.ai/api/department.md` §2·§3·§4).
 *
 * <p>세 API 모두 <b>ADMIN 전용</b>이다. Security 필터는 인증(세션 유무)만 보므로
 * ADMIN 판정은 여기서 코드({@code ACC_ADMIN_REQUIRED})와 함께 명시적으로 한다 —
 * 명세가 일반 403 이 아니라 도메인 코드를 요구하기 때문이다 (account 도메인과 같은 방식).
 *
 * <p>쓰기는 JPA, 사원 집계처럼 {@code employee} 를 가로지르는 조회는 MyBatis({@link DepartmentMapper}) 다.
 * 조회(트리 조립)는 {@link DepartmentQueryService} 가 맡는다.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DepartmentCommandService {

    /** 부서명 최대 길이 (`.ai/api/department.md` §2·§3) */
    private static final int MAX_NAME_LENGTH = 50;
    private static final String ADMIN = "ADMIN";

    private final DepartmentJpaRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    /**
     * 부서 생성 (`.ai/api/department.md` §2).
     *
     * <p>{@code parentId} 유무로 최상위/하위가 갈린다. 하위 부서를 상위로 지정하면 계층이 3단이 되므로
     * {@code DEPT_MAX_DEPTH_EXCEEDED}(409) 로 막는다. 부서명은 <b>전체에서</b> 유니크해야 한다 —
     * MySQL 은 {@code parent_id} 가 {@code NULL} 인 행끼리 중복을 허용해 최상위 부서명 중복이 막히지 않기 때문.
     */
    public DepartmentCreateResponse create(String currentUserRole, String name, Long parentId) {
        requireAdmin(currentUserRole);
        validateName(name);

        String parentName = null;
        if (parentId != null) {
            DepartmentEntity parent = departmentRepository.findById(parentId)
                    .orElseThrow(() -> new NotFoundException(DepartmentErrorCode.DEPT_PARENT_NOT_FOUND));
            if (!parent.isRoot()) {
                throw new ConflictException(DepartmentErrorCode.DEPT_MAX_DEPTH_EXCEEDED);
            }
            parentName = parent.getName();
        }
        if (departmentRepository.existsByName(name)) {
            throw new ConflictException(DepartmentErrorCode.DEPT_NAME_DUPLICATED);
        }

        DepartmentEntity saved = departmentRepository.save(DepartmentEntity.create(name, parentId));
        log.info("부서 생성 — departmentId={} name={} parentId={}", saved.getDepartmentId(), name, parentId);
        return new DepartmentCreateResponse(
                saved.getDepartmentId(), saved.getName(), parentId, parentName, 0, 0);
    }

    /**
     * 부서명 수정 (`.ai/api/department.md` §3).
     *
     * <p>⛔ 상위 부서는 바꾸지 않는다 — 부서 이동 기능이 없다. 이름만 바꾼다.
     * 유니크 검증은 자기 자신을 제외해, 이름을 그대로 다시 저장해도 중복으로 튕기지 않게 한다.
     */
    public DepartmentUpdateResponse rename(String currentUserRole, Long departmentId, String name) {
        requireAdmin(currentUserRole);
        validateName(name);

        DepartmentEntity department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new NotFoundException(DepartmentErrorCode.DEPT_NOT_FOUND));
        if (departmentRepository.existsByNameAndDepartmentIdNot(name, departmentId)) {
            throw new ConflictException(DepartmentErrorCode.DEPT_NAME_DUPLICATED);
        }

        department.rename(name);
        log.info("부서명 수정 — departmentId={} name={}", departmentId, name);
        return new DepartmentUpdateResponse(
                department.getDepartmentId(), department.getName(),
                department.getParentId(), resolveParentName(department.getParentId()));
    }

    /**
     * 부서 삭제 (`.ai/api/department.md` §4).
     *
     * <p>차단 조건이 둘이다 — 직속 사원이 있거나 하위 부서가 있으면 지우지 않는다. 하위 부서를
     * CASCADE 로 함께 지우면 팀 하나 지우려다 본부 전체가 사라진다. 소프트 삭제가 아니라 행을 제거한다.
     * 프론트가 "왜 막혔는지" 를 보여줄 수 있게 {@code message} 에 개수를 담는다.
     */
    public void delete(String currentUserRole, Long departmentId) {
        requireAdmin(currentUserRole);

        DepartmentEntity department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new NotFoundException(DepartmentErrorCode.DEPT_NOT_FOUND));

        long directEmployees = departmentMapper.countDirectEmployees(departmentId);
        if (directEmployees > 0) {
            throw new ConflictException(DepartmentErrorCode.DEPT_HAS_EMPLOYEES,
                    "소속 사원 " + directEmployees + "명이 있어 삭제할 수 없습니다.");
        }
        long children = departmentRepository.countByParentId(departmentId);
        if (children > 0) {
            throw new ConflictException(DepartmentErrorCode.DEPT_HAS_CHILDREN,
                    "하위 부서 " + children + "개가 있어 삭제할 수 없습니다.");
        }

        departmentRepository.delete(department);
        log.info("부서 삭제 — departmentId={}", departmentId);
    }

    // ===== 공통 =====

    private void requireAdmin(String currentUserRole) {
        if (!ADMIN.equals(currentUserRole)) {
            throw new ForbiddenException(AccountErrorCode.ACC_ADMIN_REQUIRED);
        }
    }

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
                .map(DepartmentEntity::getName)
                .orElse(null);
    }
}
