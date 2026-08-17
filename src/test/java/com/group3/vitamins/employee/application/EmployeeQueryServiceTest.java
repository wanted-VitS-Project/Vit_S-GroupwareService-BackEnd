package com.group3.vitamins.employee.application;

import com.group3.vitamins.employee.application.port.EmployeeSearchQueryPort;
import com.group3.vitamins.employee.application.query.EmployeeSearchQuery;
import com.group3.vitamins.employee.application.result.EmployeeSearchRow;
import com.group3.vitamins.employee.application.service.EmployeeQueryService;
import com.group3.vitamins.employee.domain.exception.EmployeeErrorCode;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("EmployeeQueryService 사원 이름 검색")
class EmployeeQueryServiceTest {

    private EmployeeSearchQueryPort searchPort;
    private CurrentCompanyIdProvider currentCompanyIdProvider;
    private EmployeeQueryService queryService;

    @BeforeEach
    void setUp() {
        searchPort = Mockito.mock(EmployeeSearchQueryPort.class);
        currentCompanyIdProvider = Mockito.mock(CurrentCompanyIdProvider.class);
        when(currentCompanyIdProvider.currentCompanyId()).thenReturn(1L);
        queryService = new EmployeeQueryService(searchPort, currentCompanyIdProvider);
    }

    @Test
    @DisplayName("이름으로 검색하면 후보 목록을 그대로 반환한다")
    void returnsCandidates() {
        when(searchPort.searchByName("김", 1L)).thenReturn(List.of(
                new EmployeeSearchRow("EMP001", "김민준", "개발팀", "대리", "profile-images/EMP001/a.png"),
                new EmployeeSearchRow("EMP007", "김서연", null, null, null)
        ));

        List<EmployeeSearchRow> result = queryService.searchByName(new EmployeeSearchQuery("김"));

        assertThat(result).extracting(EmployeeSearchRow::userId).containsExactly("EMP001", "EMP007");
        assertThat(result.get(1).department()).isNull();
    }

    @Test
    @DisplayName("검색어 앞뒤 공백은 trim 되어 포트에 넘어간다")
    void trimsWhitespace() {
        when(searchPort.searchByName("김민준", 1L)).thenReturn(List.of());

        queryService.searchByName(new EmployeeSearchQuery("  김민준  "));

        verify(searchPort).searchByName("김민준", 1L);
    }

    @Test
    @DisplayName("결과가 없으면 빈 배열")
    void emptyWhenNoMatch() {
        when(searchPort.searchByName("없는이름", 1L)).thenReturn(List.of());

        assertThat(queryService.searchByName(new EmployeeSearchQuery("없는이름"))).isEmpty();
    }

    @Test
    @DisplayName("검색어가 null 이면 EMP_INVALID_PARAMETER — 조회하지 않는다")
    void rejectsNullName() {
        assertThatThrownBy(() -> queryService.searchByName(new EmployeeSearchQuery(null)))
                .satisfies(hasCode(EmployeeErrorCode.EMP_INVALID_PARAMETER));
        verify(searchPort, never()).searchByName(any(), anyLong());
    }

    @Test
    @DisplayName("검색어가 공백뿐이면 EMP_INVALID_PARAMETER (쿼리에서 null 로 눕는다)")
    void rejectsBlankName() {
        assertThatThrownBy(() -> queryService.searchByName(new EmployeeSearchQuery("   ")))
                .satisfies(hasCode(EmployeeErrorCode.EMP_INVALID_PARAMETER));
        verify(searchPort, never()).searchByName(any(), anyLong());
    }

    @Test
    @DisplayName("query 자체가 null 이면 NPE 가 아니라 EMP_INVALID_PARAMETER")
    void rejectsNullQuery() {
        assertThatThrownBy(() -> queryService.searchByName(null))
                .satisfies(hasCode(EmployeeErrorCode.EMP_INVALID_PARAMETER));
        verify(searchPort, never()).searchByName(any(), anyLong());
    }

    private Consumer<Throwable> hasCode(Object expected) {
        return throwable -> assertThat(throwable)
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
