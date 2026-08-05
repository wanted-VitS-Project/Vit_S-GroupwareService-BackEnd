package com.group3.vitamins.department.application;

import com.group3.vitamins.department.application.port.DepartmentEmployeeQueryPort;
import com.group3.vitamins.department.application.result.DepartmentEmployeeCountRow;
import com.group3.vitamins.department.application.result.DepartmentTreeResult;
import com.group3.vitamins.department.application.service.DepartmentQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("DepartmentQueryService 부서 트리 조립")
class DepartmentQueryServiceTest {

    private DepartmentEmployeeQueryPort departmentEmployeeQueryPort;
    private DepartmentQueryService queryService;

    @BeforeEach
    void setUp() {
        departmentEmployeeQueryPort = Mockito.mock(DepartmentEmployeeQueryPort.class);
        queryService = new DepartmentQueryService(departmentEmployeeQueryPort);
    }

    @Test
    @DisplayName("최상위-하위 2단으로 조립하고 상위 인원은 자식 직속 합으로 롤업한다")
    void assemblesTwoLevelTreeAndRollsUpTotal() {
        // 경영지원본부(직속0) > 인사팀(2)·회계팀(2) / 기술본부(직속1) > 개발팀(5)
        when(departmentEmployeeQueryPort.findAllWithDirectEmployeeCount()).thenReturn(List.of(
                new DepartmentEmployeeCountRow(1L, "경영지원본부", null, 0),
                new DepartmentEmployeeCountRow(2L, "기술본부", null, 1),
                new DepartmentEmployeeCountRow(4L, "인사팀", 1L, 2),
                new DepartmentEmployeeCountRow(5L, "회계팀", 1L, 2),
                new DepartmentEmployeeCountRow(6L, "개발팀", 2L, 5)
        ));

        List<DepartmentTreeResult> result = queryService.getDepartmentTree();

        // 최상위는 2개, 생성 순서(departmentId) 유지
        assertThat(result).extracting(DepartmentTreeResult::departmentId)
                .containsExactly(1L, 2L);

        DepartmentTreeResult management = result.get(0);
        assertThat(management.directEmployeeCount()).isEqualTo(0);
        assertThat(management.totalEmployeeCount()).isEqualTo(4); // 0 + 2 + 2
        assertThat(management.children()).extracting(DepartmentTreeResult::name)
                .containsExactly("인사팀", "회계팀");

        DepartmentTreeResult tech = result.get(1);
        assertThat(tech.totalEmployeeCount()).isEqualTo(6); // 1 + 5
    }

    @Test
    @DisplayName("하위 부서의 total 은 자기 직속과 같고 children 은 빈 배열이다")
    void leafTotalEqualsDirectAndChildrenEmpty() {
        when(departmentEmployeeQueryPort.findAllWithDirectEmployeeCount()).thenReturn(List.of(
                new DepartmentEmployeeCountRow(1L, "경영지원본부", null, 0),
                new DepartmentEmployeeCountRow(4L, "인사팀", 1L, 3)
        ));

        DepartmentTreeResult leaf = queryService.getDepartmentTree().get(0).children().get(0);

        assertThat(leaf.directEmployeeCount()).isEqualTo(3);
        assertThat(leaf.totalEmployeeCount()).isEqualTo(3);
        assertThat(leaf.children()).isEmpty();
    }

    @Test
    @DisplayName("하위 부서가 없는 최상위는 children 이 빈 배열이고 total = 직속이다")
    void rootWithoutChildren() {
        when(departmentEmployeeQueryPort.findAllWithDirectEmployeeCount()).thenReturn(List.of(
                new DepartmentEmployeeCountRow(3L, "감사실", null, 2)
        ));

        DepartmentTreeResult root = queryService.getDepartmentTree().get(0);

        assertThat(root.children()).isEmpty();
        assertThat(root.totalEmployeeCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("부서가 하나도 없으면 빈 배열이다")
    void emptyWhenNoDepartments() {
        when(departmentEmployeeQueryPort.findAllWithDirectEmployeeCount()).thenReturn(List.of());

        assertThat(queryService.getDepartmentTree()).isEmpty();
    }
}
