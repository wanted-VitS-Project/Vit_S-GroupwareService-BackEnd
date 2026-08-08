package com.group3.vitamins.employee.infrastructure.adapter;

import com.group3.vitamins.employee.application.port.EmployeeBulkReferenceQueryPort;
import com.group3.vitamins.employee.application.result.NameIdRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link EmployeeBulkReferenceQueryPort} 구현 — 이름 목록을 한 번에 조회해 name→id 맵으로 만든다.
 * ⚠️ 빈 컬렉션이면 쿼리를 건너뛴다({@code IN ()} 방지)·결과는 빈 맵.
 */
@Component
@RequiredArgsConstructor
public class EmployeeBulkReferenceQueryAdapter implements EmployeeBulkReferenceQueryPort {

    private final EmployeeBulkReferenceQueryMapper mapper;

    @Override
    public Map<String, Long> resolveDepartmentIdsByName(Collection<String> names) {
        return toMap(names, mapper::findUniqueDepartmentIdsByName);
    }

    @Override
    public Map<String, Long> resolveJobPositionIdsByName(Collection<String> names) {
        return toMap(names, mapper::findJobPositionIdsByName);
    }

    private Map<String, Long> toMap(Collection<String> names,
                                    java.util.function.Function<Collection<String>, java.util.List<NameIdRow>> query) {
        if (names == null || names.isEmpty()) {
            return Map.of();
        }
        return query.apply(names).stream()
                .collect(Collectors.toMap(NameIdRow::name, NameIdRow::id));
    }
}
