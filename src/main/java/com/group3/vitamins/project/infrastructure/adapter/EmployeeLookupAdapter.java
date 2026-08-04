package com.group3.vitamins.project.infrastructure.adapter;

import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
public class EmployeeLookupAdapter implements EmployeeLookupPort {

    private final EmployeeLookupQueryMapper employeeLookupQueryMapper;

    @Override
    public String findNameByUserId(String userId) {
        return employeeLookupQueryMapper.findNameByUserId(userId).orElse(null);
    }

    @Override
    public Map<String, String> findNamesByUserIds(Collection<String> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return employeeLookupQueryMapper.findNamesByUserIds(userIds).stream()
                .collect(Collectors.toMap(EmployeeNameRow::userId, EmployeeNameRow::name));
    }
}