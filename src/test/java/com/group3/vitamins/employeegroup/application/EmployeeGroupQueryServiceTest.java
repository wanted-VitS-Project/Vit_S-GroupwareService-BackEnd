package com.group3.vitamins.employeegroup.application;

import com.group3.vitamins.employeegroup.application.port.EmployeeGroupQueryPort;
import com.group3.vitamins.employeegroup.application.result.GroupListRow;
import com.group3.vitamins.employeegroup.application.result.GroupMembersResult;
import com.group3.vitamins.employeegroup.application.result.MemberRow;
import com.group3.vitamins.employeegroup.application.service.EmployeeGroupQueryService;
import com.group3.vitamins.employeegroup.domain.exception.EmployeeGroupErrorCode;
import com.group3.vitamins.global.domain.common.error.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("EmployeeGroupQueryService")
class EmployeeGroupQueryServiceTest {

    private static final Long GROUP_ID = 7L;

    private EmployeeGroupQueryPort queryPort;
    private EmployeeGroupQueryService service;

    @BeforeEach
    void setUp() {
        queryPort = Mockito.mock(EmployeeGroupQueryPort.class);
        service = new EmployeeGroupQueryService(queryPort);
    }

    private GroupListRow group() {
        return new GroupListRow(GROUP_ID, "개발팀", null, 2, "관리자", LocalDateTime.now());
    }

    @Test
    @DisplayName("목록 조회 시 공백 keyword 는 null 로 정규화해 넘긴다")
    void listNormalizesKeyword() {
        when(queryPort.findGroups(any())).thenReturn(List.of());
        service.listGroups("   ");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(queryPort).findGroups(captor.capture());
        assertThat(captor.getValue()).isNull();
    }

    @Test
    @DisplayName("단건 조회에서 없으면 GRP_NOT_FOUND")
    void getGroupNotFound() {
        when(queryPort.findGroup(GROUP_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getGroup(GROUP_ID))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(EmployeeGroupErrorCode.GRP_NOT_FOUND);
    }

    @Test
    @DisplayName("구성원 목록에서 그룹이 없으면 GRP_NOT_FOUND")
    void membersGroupNotFound() {
        when(queryPort.findGroup(GROUP_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getMembers(GROUP_ID))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(EmployeeGroupErrorCode.GRP_NOT_FOUND);
    }

    @Test
    @DisplayName("departmentPath 를 상위부서 유무에 따라 조립한다")
    void assemblesDepartmentPath() {
        when(queryPort.findGroup(GROUP_ID)).thenReturn(Optional.of(group()));
        when(queryPort.findMembers(GROUP_ID)).thenReturn(List.of(
                new MemberRow("EMP1", "홍길동", "개발팀", "기술본부", "대리", LocalDateTime.now()),  // 상위 있음
                new MemberRow("EMP2", "김철수", "인사팀", null, null, LocalDateTime.now()),          // 상위 없음
                new MemberRow("EMP3", "이영희", null, null, "사원", LocalDateTime.now())));          // 부서 없음

        GroupMembersResult r = service.getMembers(GROUP_ID);

        assertThat(r.content()).extracting("departmentPath")
                .containsExactly("기술본부 / 개발팀", "인사팀", null);
    }
}
