package com.group3.vitamins.department.application;

import com.group3.vitamins.account.domain.exception.AccountErrorCode;
import com.group3.vitamins.department.domain.exception.DepartmentErrorCode;
import com.group3.vitamins.department.infrastructure.persistence.DepartmentEntity;
import com.group3.vitamins.department.infrastructure.persistence.DepartmentJpaRepository;
import com.group3.vitamins.department.infrastructure.persistence.mapper.DepartmentMapper;
import com.group3.vitamins.department.presentation.api.dto.response.DepartmentCreateResponse;
import com.group3.vitamins.department.presentation.api.dto.response.DepartmentUpdateResponse;
import com.group3.vitamins.global.domain.common.error.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
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

@DisplayName("DepartmentCommandService 부서 생성·수정·삭제")
class DepartmentCommandServiceTest {

    private DepartmentJpaRepository departmentRepository;
    private DepartmentMapper departmentMapper;
    private DepartmentCommandService commandService;

    @BeforeEach
    void setUp() {
        departmentRepository = Mockito.mock(DepartmentJpaRepository.class);
        departmentMapper = Mockito.mock(DepartmentMapper.class);
        commandService = new DepartmentCommandService(departmentRepository, departmentMapper);
    }

    /** id 가 설정된 부서 엔티티를 만든다 (JPA 가 채우는 departmentId 를 테스트에서 흉내낸다). */
    private DepartmentEntity department(Long id, String name, Long parentId) {
        DepartmentEntity entity = DepartmentEntity.create(name, parentId);
        ReflectionTestUtils.setField(entity, "departmentId", id);
        return entity;
    }

    @Nested
    @DisplayName("부서 생성")
    class Create {

        @Test
        @DisplayName("최상위 부서를 생성하면 parentName·인원 수가 없고(0) 저장된다")
        void createsRootDepartment() {
            when(departmentRepository.existsByName("신규본부")).thenReturn(false);
            when(departmentRepository.save(any())).thenReturn(department(10L, "신규본부", null));

            DepartmentCreateResponse result = commandService.create("ADMIN", "신규본부", null);

            assertThat(result.departmentId()).isEqualTo(10L);
            assertThat(result.name()).isEqualTo("신규본부");
            assertThat(result.parentId()).isNull();
            assertThat(result.parentName()).isNull();
            assertThat(result.directEmployeeCount()).isZero();
            assertThat(result.totalEmployeeCount()).isZero();
            verify(departmentRepository, times(1)).save(any(DepartmentEntity.class));
        }

        @Test
        @DisplayName("하위 부서를 생성하면 상위 부서명(parentName)이 응답에 담긴다")
        void createsChildDepartment() {
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(department(1L, "경영지원본부", null)));
            when(departmentRepository.existsByName("인사팀")).thenReturn(false);
            when(departmentRepository.save(any())).thenReturn(department(11L, "인사팀", 1L));

            DepartmentCreateResponse result = commandService.create("ADMIN", "인사팀", 1L);

            assertThat(result.departmentId()).isEqualTo(11L);
            assertThat(result.parentId()).isEqualTo(1L);
            assertThat(result.parentName()).isEqualTo("경영지원본부");
        }

        @Test
        @DisplayName("ADMIN 이 아니면 ACC_ADMIN_REQUIRED — 조회·저장 이전에 막는다")
        void rejectsNonAdmin() {
            assertThatThrownBy(() -> commandService.create("MASTER", "인사팀", null))
                    .satisfies(hasCode(AccountErrorCode.ACC_ADMIN_REQUIRED));
            verify(departmentRepository, never()).save(any());
            verify(departmentRepository, never()).existsByName(anyString());
        }

        @Test
        @DisplayName("부서명이 비어 있으면 DEPT_INVALID_REQUEST")
        void rejectsBlankName() {
            assertThatThrownBy(() -> commandService.create("ADMIN", "  ", null))
                    .satisfies(hasCode(DepartmentErrorCode.DEPT_INVALID_REQUEST));
        }

        @Test
        @DisplayName("부서명이 50자를 초과하면 DEPT_INVALID_REQUEST")
        void rejectsTooLongName() {
            String tooLong = "가".repeat(51);
            assertThatThrownBy(() -> commandService.create("ADMIN", tooLong, null))
                    .satisfies(hasCode(DepartmentErrorCode.DEPT_INVALID_REQUEST));
        }

        @Test
        @DisplayName("상위 부서가 없으면 DEPT_PARENT_NOT_FOUND")
        void rejectsMissingParent() {
            when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commandService.create("ADMIN", "인사팀", 99L))
                    .satisfies(hasCode(DepartmentErrorCode.DEPT_PARENT_NOT_FOUND));
        }

        @Test
        @DisplayName("하위 부서를 상위로 지정하면 계층 2단 초과 — DEPT_MAX_DEPTH_EXCEEDED")
        void rejectsThirdLevel() {
            // 인사팀(parentId=1)은 이미 하위 부서다. 이걸 상위로 지정하면 3단이 된다.
            when(departmentRepository.findById(4L)).thenReturn(Optional.of(department(4L, "인사팀", 1L)));

            assertThatThrownBy(() -> commandService.create("ADMIN", "인사1파트", 4L))
                    .satisfies(hasCode(DepartmentErrorCode.DEPT_MAX_DEPTH_EXCEEDED));
            verify(departmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미 존재하는 부서명이면 DEPT_NAME_DUPLICATED")
        void rejectsDuplicateName() {
            when(departmentRepository.existsByName("인사팀")).thenReturn(true);

            assertThatThrownBy(() -> commandService.create("ADMIN", "인사팀", null))
                    .satisfies(hasCode(DepartmentErrorCode.DEPT_NAME_DUPLICATED));
            verify(departmentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("부서명 수정")
    class Rename {

        @Test
        @DisplayName("부서명을 수정하면 이름이 바뀌고 상위 정보가 그대로 담긴다")
        void renamesDepartment() {
            when(departmentRepository.findById(4L)).thenReturn(Optional.of(department(4L, "인사팀", 1L)));
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(department(1L, "경영지원본부", null)));
            when(departmentRepository.existsByNameAndDepartmentIdNot("인사기획팀", 4L)).thenReturn(false);

            DepartmentUpdateResponse result = commandService.rename("ADMIN", 4L, "인사기획팀");

            assertThat(result.departmentId()).isEqualTo(4L);
            assertThat(result.name()).isEqualTo("인사기획팀");
            assertThat(result.parentId()).isEqualTo(1L);
            assertThat(result.parentName()).isEqualTo("경영지원본부");
        }

        @Test
        @DisplayName("부서가 없으면 DEPT_NOT_FOUND")
        void rejectsMissingDepartment() {
            when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commandService.rename("ADMIN", 99L, "새이름"))
                    .satisfies(hasCode(DepartmentErrorCode.DEPT_NOT_FOUND));
        }

        @Test
        @DisplayName("다른 부서와 이름이 겹치면 DEPT_NAME_DUPLICATED")
        void rejectsDuplicateName() {
            when(departmentRepository.findById(4L)).thenReturn(Optional.of(department(4L, "인사팀", 1L)));
            when(departmentRepository.existsByNameAndDepartmentIdNot("회계팀", 4L)).thenReturn(true);

            assertThatThrownBy(() -> commandService.rename("ADMIN", 4L, "회계팀"))
                    .satisfies(hasCode(DepartmentErrorCode.DEPT_NAME_DUPLICATED));
        }

        @Test
        @DisplayName("ADMIN 이 아니면 ACC_ADMIN_REQUIRED")
        void rejectsNonAdmin() {
            assertThatThrownBy(() -> commandService.rename("MEMBER", 4L, "새이름"))
                    .satisfies(hasCode(AccountErrorCode.ACC_ADMIN_REQUIRED));
            verify(departmentRepository, never()).findById(anyLong());
        }
    }

    @Nested
    @DisplayName("부서 삭제")
    class Delete {

        @Test
        @DisplayName("직속 사원·하위 부서가 없으면 삭제된다")
        void deletesDepartment() {
            DepartmentEntity target = department(5L, "회계팀", 1L);
            when(departmentRepository.findById(5L)).thenReturn(Optional.of(target));
            when(departmentMapper.countDirectEmployees(5L)).thenReturn(0L);
            when(departmentRepository.countByParentId(5L)).thenReturn(0L);

            commandService.delete("ADMIN", 5L);

            verify(departmentRepository, times(1)).delete(target);
        }

        @Test
        @DisplayName("부서가 없으면 DEPT_NOT_FOUND")
        void rejectsMissingDepartment() {
            when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commandService.delete("ADMIN", 99L))
                    .satisfies(hasCode(DepartmentErrorCode.DEPT_NOT_FOUND));
        }

        @Test
        @DisplayName("직속 사원이 있으면 DEPT_HAS_EMPLOYEES — 메시지에 인원 수를 담는다")
        void rejectsWhenHasEmployees() {
            when(departmentRepository.findById(4L)).thenReturn(Optional.of(department(4L, "인사팀", 1L)));
            when(departmentMapper.countDirectEmployees(4L)).thenReturn(3L);

            assertThatThrownBy(() -> commandService.delete("ADMIN", 4L))
                    .satisfies(hasCode(DepartmentErrorCode.DEPT_HAS_EMPLOYEES))
                    .hasMessageContaining("3");
            verify(departmentRepository, never()).delete(any());
        }

        @Test
        @DisplayName("하위 부서가 있으면 DEPT_HAS_CHILDREN — 메시지에 하위 부서 수를 담는다")
        void rejectsWhenHasChildren() {
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(department(1L, "경영지원본부", null)));
            when(departmentMapper.countDirectEmployees(1L)).thenReturn(0L);
            when(departmentRepository.countByParentId(1L)).thenReturn(2L);

            assertThatThrownBy(() -> commandService.delete("ADMIN", 1L))
                    .satisfies(hasCode(DepartmentErrorCode.DEPT_HAS_CHILDREN))
                    .hasMessageContaining("2");
            verify(departmentRepository, never()).delete(any());
        }

        @Test
        @DisplayName("ADMIN 이 아니면 ACC_ADMIN_REQUIRED")
        void rejectsNonAdmin() {
            assertThatThrownBy(() -> commandService.delete("MASTER", 5L))
                    .satisfies(hasCode(AccountErrorCode.ACC_ADMIN_REQUIRED));
            verify(departmentRepository, never()).findById(anyLong());
        }
    }

    private Consumer<Throwable> hasCode(Object expected) {
        return throwable -> assertThat(throwable)
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
