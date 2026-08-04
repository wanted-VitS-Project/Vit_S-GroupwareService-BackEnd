package com.group3.vitamins.project.infrastructure.adapter;

import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmployeeLookupAdapter implements EmployeeLookupPort {

    private final EmployeeLookupQueryMapper employeeLookupQueryMapper;

    @Override
    public String findNameByUserId(String userId) {
        return employeeLookupQueryMapper.findNameByUserId(userId).orElse(null);
    }
}