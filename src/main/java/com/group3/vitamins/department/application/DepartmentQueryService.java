package com.group3.vitamins.department.application;

import com.group3.vitamins.department.infrastructure.persistence.DepartmentTreeRow;
import com.group3.vitamins.department.infrastructure.persistence.mapper.DepartmentMapper;
import com.group3.vitamins.department.presentation.api.dto.response.DepartmentListResponse;
import com.group3.vitamins.department.presentation.api.dto.response.DepartmentTreeResponse;
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
public class DepartmentQueryService {

    private final DepartmentMapper departmentMapper;

    /**
     * 전체 부서를 최대 2단 트리로 조립해 반환한다.
     *
     * <p>매퍼는 부서별 <b>직속</b> 인원만 센 평면 행을 준다. 여기서 최상위/하위로 나눠 조립하며
     * 상위의 {@code totalEmployeeCount} = 자기 직속 + 자식들 직속으로 계산한다 (계층이 2단이라 이걸로 충분).
     * 정렬은 매퍼의 {@code department_id} 오름차순을 그대로 유지한다.
     */
    public DepartmentListResponse getDepartmentTree() {
        List<DepartmentTreeRow> rows = departmentMapper.findAllWithDirectEmployeeCount();

        // parentId 별 자식 묶음. 원본이 department_id 오름차순이라 삽입 순서가 곧 정렬이다.
        Map<Long, List<DepartmentTreeRow>> childrenByParent = new LinkedHashMap<>();
        for (DepartmentTreeRow row : rows) {
            if (row.parentId() != null) {
                childrenByParent.computeIfAbsent(row.parentId(), key -> new ArrayList<>()).add(row);
            }
        }

        List<DepartmentTreeResponse> content = new ArrayList<>();
        for (DepartmentTreeRow row : rows) {
            if (row.parentId() != null) {
                continue; // 최상위만 순회 — 자식은 아래에서 부모에 매단다
            }
            List<DepartmentTreeRow> childRows = childrenByParent.getOrDefault(row.departmentId(), List.of());

            List<DepartmentTreeResponse> children = new ArrayList<>();
            int childrenTotal = 0;
            for (DepartmentTreeRow child : childRows) {
                // 하위 부서는 다시 자식을 갖지 않는다 (최대 2단) → children 은 빈 배열, total = 직속
                children.add(new DepartmentTreeResponse(
                        child.departmentId(), child.name(),
                        child.directEmployeeCount(), child.directEmployeeCount(), List.of()));
                childrenTotal += child.directEmployeeCount();
            }

            content.add(new DepartmentTreeResponse(
                    row.departmentId(), row.name(),
                    row.directEmployeeCount(), row.directEmployeeCount() + childrenTotal, children));
        }

        return new DepartmentListResponse(content);
    }
}
