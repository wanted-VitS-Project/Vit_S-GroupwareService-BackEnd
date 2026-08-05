package com.group3.vitamins.department.application.service;

import com.group3.vitamins.department.application.port.DepartmentEmployeeQueryPort;
import com.group3.vitamins.department.application.result.DepartmentEmployeeCountRow;
import com.group3.vitamins.department.application.result.DepartmentTreeResult;
import com.group3.vitamins.department.application.usecase.DepartmentQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 부서 조회 유스케이스 — 목록 트리 (`.ai/api/department.md` §1).
 *
 * <p>권한은 <b>전체 사용자</b>다. 사원 등록·수정의 부서 드롭다운, 사원 목록 필터,
 * 구성원 추가 모달 등에서 쓰이므로 ADMIN 으로 좁히면 그 화면들이 막힌다.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DepartmentQueryService implements DepartmentQueryUseCase {

    private final DepartmentEmployeeQueryPort departmentEmployeeQueryPort;

    /**
     * 전체 부서를 최대 2단 트리로 조립해 반환한다.
     *
     * <p>포트는 부서별 <b>직속</b> 인원만 센 평면 행을 준다. 여기서 최상위/하위로 나눠 조립하며
     * 상위의 {@code totalEmployeeCount} = 자기 직속 + 자식들 직속으로 계산한다 (계층이 2단이라 이걸로 충분).
     * 정렬은 포트의 {@code department_id} 오름차순을 그대로 유지한다.
     */
    @Override
    public List<DepartmentTreeResult> getDepartmentTree() {
        List<DepartmentEmployeeCountRow> rows = departmentEmployeeQueryPort.findAllWithDirectEmployeeCount();

        // parentId 별 자식 묶음. 원본이 department_id 오름차순이라 삽입 순서가 곧 정렬이다.
        Map<Long, List<DepartmentEmployeeCountRow>> childrenByParent = new LinkedHashMap<>();
        for (DepartmentEmployeeCountRow row : rows) {
            if (row.parentId() != null) {
                childrenByParent.computeIfAbsent(row.parentId(), key -> new ArrayList<>()).add(row);
            }
        }

        List<DepartmentTreeResult> content = new ArrayList<>();
        for (DepartmentEmployeeCountRow row : rows) {
            if (row.parentId() != null) {
                continue; // 최상위만 순회 — 자식은 아래에서 부모에 매단다
            }
            List<DepartmentEmployeeCountRow> childRows =
                    childrenByParent.getOrDefault(row.departmentId(), List.of());

            List<DepartmentTreeResult> children = new ArrayList<>();
            int childrenTotal = 0;
            for (DepartmentEmployeeCountRow child : childRows) {
                // 하위 부서는 다시 자식을 갖지 않는다 (최대 2단) → children 은 빈 배열, total = 직속
                children.add(new DepartmentTreeResult(
                        child.departmentId(), child.name(),
                        child.directEmployeeCount(), child.directEmployeeCount(), List.of()));
                childrenTotal += child.directEmployeeCount();
            }

            content.add(new DepartmentTreeResult(
                    row.departmentId(), row.name(),
                    row.directEmployeeCount(), row.directEmployeeCount() + childrenTotal, children));
        }

        return content;
    }
}
