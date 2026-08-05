package com.group3.vitamins.employee.application;

import com.group3.vitamins.account.domain.exception.AccountErrorCode;
import com.group3.vitamins.employee.application.policy.EmployeeAdminPolicy;
import com.group3.vitamins.employee.application.port.EmployeeAdminQueryPort;
import com.group3.vitamins.employee.application.query.EmployeeListCriteria;
import com.group3.vitamins.employee.application.query.EmployeeListQuery;
import com.group3.vitamins.employee.application.result.EmployeeDetailRow;
import com.group3.vitamins.employee.application.result.EmployeeGroupRow;
import com.group3.vitamins.employee.application.result.EmployeeListRow;
import com.group3.vitamins.employee.application.result.EmployeePage;
import com.group3.vitamins.employee.application.service.EmployeeAdminQueryService;
import com.group3.vitamins.employee.application.usecase.EmployeeAdminQueryUseCase;
import com.group3.vitamins.employee.domain.exception.EmployeeErrorCode;
import com.group3.vitamins.global.domain.common.error.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("EmployeeAdminQueryService 인사관리 사원 조회")
class EmployeeAdminQueryServiceTest {

    private EmployeeAdminQueryPort port;
    private EmployeeAdminQueryService service;

    @BeforeEach
    void setUp() {
        port = Mockito.mock(EmployeeAdminQueryPort.class);
        service = new EmployeeAdminQueryService(port, new EmployeeAdminPolicy());
    }

    @Nested
    @DisplayName("목록 조회")
    class ListEmployees {

        @Test
        @DisplayName("ADMIN 이 아니면 ACC_ADMIN_REQUIRED — 조회하지 않는다")
        void rejectsNonAdmin() {
            assertThatThrownBy(() -> service.listEmployees(query("MASTER", null, null, null, 0, 20)))
                    .satisfies(hasCode(AccountErrorCode.ACC_ADMIN_REQUIRED));
            verify(port, never()).count(any());
            verify(port, never()).findPage(any());
        }

        @Test
        @DisplayName("건수가 0 이면 findPage 를 부르지 않고 빈 목록을 반환한다")
        void emptyWhenCountZero() {
            when(port.count(any())).thenReturn(0L);

            EmployeePage page = service.listEmployees(query("ADMIN", null, null, null, 0, 20));

            assertThat(page.content()).isEmpty();
            assertThat(page.totalElements()).isZero();
            assertThat(page.totalPages()).isZero();
            verify(port, never()).findPage(any());
        }

        @Test
        @DisplayName("건수가 있으면 페이지 내용과 메타를 조립한다")
        void assemblesPage() {
            when(port.count(any())).thenReturn(42L);
            when(port.findPage(any())).thenReturn(List.of(listRow("EMP001"), listRow("EMP002")));

            EmployeePage page = service.listEmployees(query("ADMIN", null, null, null, 1, 20));

            assertThat(page.content()).hasSize(2);
            assertThat(page.page()).isEqualTo(1);
            assertThat(page.size()).isEqualTo(20);
            assertThat(page.totalElements()).isEqualTo(42L);
            assertThat(page.totalPages()).isEqualTo(3); // ceil(42/20)
        }

        @Test
        @DisplayName("status=RESET_REQUIRED 는 (ACTIVE · 비번변경필요) 로 풀리고 offset=page*size 로 넘어간다")
        void translatesStatusAndPaging() {
            when(port.count(any())).thenReturn(1L);
            when(port.findPage(any())).thenReturn(List.of(listRow("EMP001")));

            service.listEmployees(query("ADMIN", null, "RESET_REQUIRED", null, 2, 15));

            ArgumentCaptor<EmployeeListCriteria> captor = ArgumentCaptor.forClass(EmployeeListCriteria.class);
            verify(port).findPage(captor.capture());
            EmployeeListCriteria c = captor.getValue();
            assertThat(c.accountStatus()).isEqualTo("ACTIVE");
            assertThat(c.mustChangePassword()).isTrue();
            assertThat(c.offset()).isEqualTo(30); // 2 * 15
            assertThat(c.limit()).isEqualTo(15);
            assertThat(c.resignedOnly()).isFalse(); // resigned 미지정 → 재직자만
        }

        @Test
        @DisplayName("resigned=true 면 resignedOnly=true")
        void resignedFilter() {
            when(port.count(any())).thenReturn(1L);
            when(port.findPage(any())).thenReturn(List.of(listRow("EMP001")));

            service.listEmployees(query("ADMIN", null, null, Boolean.TRUE, 0, 20));

            ArgumentCaptor<EmployeeListCriteria> captor = ArgumentCaptor.forClass(EmployeeListCriteria.class);
            verify(port).findPage(captor.capture());
            assertThat(captor.getValue().resignedOnly()).isTrue();
        }

        @Test
        @DisplayName("허용되지 않는 role 필터는 EMP_INVALID_PARAMETER")
        void rejectsInvalidRole() {
            assertThatThrownBy(() -> service.listEmployees(query("ADMIN", "SUPERUSER", null, null, 0, 20)))
                    .satisfies(hasCode(EmployeeErrorCode.EMP_INVALID_PARAMETER));
            verify(port, never()).count(any());
        }

        @Test
        @DisplayName("허용되지 않는 status 필터는 EMP_INVALID_PARAMETER")
        void rejectsInvalidStatus() {
            assertThatThrownBy(() -> service.listEmployees(query("ADMIN", null, "DELETED", null, 0, 20)))
                    .satisfies(hasCode(EmployeeErrorCode.EMP_INVALID_PARAMETER));
            verify(port, never()).count(any());
        }

        @Test
        @DisplayName("page<0 또는 size<=0 이면 EMP_INVALID_PARAMETER")
        void rejectsInvalidPaging() {
            assertThatThrownBy(() -> service.listEmployees(query("ADMIN", null, null, null, -1, 20)))
                    .satisfies(hasCode(EmployeeErrorCode.EMP_INVALID_PARAMETER));
            assertThatThrownBy(() -> service.listEmployees(query("ADMIN", null, null, null, 0, 0)))
                    .satisfies(hasCode(EmployeeErrorCode.EMP_INVALID_PARAMETER));
            verify(port, never()).count(any());
        }

        @Test
        @DisplayName("size 가 상한(200)을 넘으면 EMP_INVALID_PARAMETER — 거대 LIMIT 차단")
        void rejectsOversizeSize() {
            assertThatThrownBy(() -> service.listEmployees(query("ADMIN", null, null, null, 0, 201)))
                    .satisfies(hasCode(EmployeeErrorCode.EMP_INVALID_PARAMETER));
            verify(port, never()).count(any());
        }

        @Test
        @DisplayName("page*size 가 int 를 넘치면 EMP_INVALID_PARAMETER — 음수 offset 방지")
        void rejectsPagingOverflow() {
            assertThatThrownBy(() -> service.listEmployees(query("ADMIN", null, null, null, Integer.MAX_VALUE, 2)))
                    .satisfies(hasCode(EmployeeErrorCode.EMP_INVALID_PARAMETER));
            verify(port, never()).count(any());
        }
    }

    @Nested
    @DisplayName("상세 조회")
    class GetEmployee {

        @Test
        @DisplayName("ADMIN 이 아니면 ACC_ADMIN_REQUIRED — 조회하지 않는다")
        void rejectsNonAdmin() {
            assertThatThrownBy(() -> service.getEmployee("MEMBER", "EMP001"))
                    .satisfies(hasCode(AccountErrorCode.ACC_ADMIN_REQUIRED));
            verify(port, never()).findDetail(any());
        }

        @Test
        @DisplayName("사원이 없으면 EMP_NOT_FOUND")
        void notFound() {
            when(port.findDetail("EMP999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getEmployee("ADMIN", "EMP999"))
                    .satisfies(hasCode(EmployeeErrorCode.EMP_NOT_FOUND));
            verify(port, never()).findGroups(any());
        }

        @Test
        @DisplayName("시스템 계정이면 ACC_SYSTEM_ACCOUNT_NOT_ALLOWED — 존재해도 403")
        void systemAccountForbidden() {
            when(port.findDetail("ADMIN001")).thenReturn(Optional.of(detailRow("ADMIN001", true)));

            assertThatThrownBy(() -> service.getEmployee("ADMIN", "ADMIN001"))
                    .satisfies(hasCode(AccountErrorCode.ACC_SYSTEM_ACCOUNT_NOT_ALLOWED));
            verify(port, never()).findGroups(any());
        }

        @Test
        @DisplayName("정상 사원이면 상세 + 소속 그룹을 함께 반환한다")
        void returnsDetailWithGroups() {
            when(port.findDetail("EMP001")).thenReturn(Optional.of(detailRow("EMP001", false)));
            when(port.findGroups("EMP001")).thenReturn(List.of(new EmployeeGroupRow(5L, "TF-신사업")));

            EmployeeAdminQueryUseCase.EmployeeDetail detail = service.getEmployee("ADMIN", "EMP001");

            assertThat(detail.employee().userId()).isEqualTo("EMP001");
            assertThat(detail.groups()).extracting(EmployeeGroupRow::name).containsExactly("TF-신사업");
        }
    }

    // --- fixtures ---

    private EmployeeListQuery query(String requesterRole, String role, String status,
                                    Boolean resigned, int page, int size) {
        return new EmployeeListQuery(requesterRole, null, null, role, status, resigned, page, size);
    }

    private EmployeeListRow listRow(String userId) {
        return new EmployeeListRow(userId, "홍길동", "hong@vitamins.com",
                "개발팀", "기술본부", "선임", "MEMBER", "ACTIVE", false, null);
    }

    private EmployeeDetailRow detailRow(String userId, boolean isSystem) {
        return new EmployeeDetailRow(userId, "홍길동", "hong@vitamins.com",
                3L, "개발팀", "기술본부", 2L, "선임", "MEMBER", "ACTIVE", false,
                "010-1234-5678", LocalDate.of(2024, 3, 2), null,
                LocalDateTime.of(2026, 8, 5, 9, 12, 33), isSystem);
    }

    private Consumer<Throwable> hasCode(Object expected) {
        return throwable -> assertThat(throwable)
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
