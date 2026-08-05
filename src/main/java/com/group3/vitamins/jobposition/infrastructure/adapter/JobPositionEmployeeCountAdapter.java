package com.group3.vitamins.jobposition.infrastructure.adapter;

import com.group3.vitamins.jobposition.application.port.JobPositionEmployeeCountPort;
import com.group3.vitamins.jobposition.application.result.JobPositionEmployeeCountRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@link JobPositionEmployeeCountPort} 의 MyBatis 어댑터. 실제 SQL 은
 * {@link JobPositionEmployeeCountMapper} 와 그 XML 이 갖는다.
 */
@Component
@RequiredArgsConstructor
public class JobPositionEmployeeCountAdapter implements JobPositionEmployeeCountPort {

    private final JobPositionEmployeeCountMapper jobPositionEmployeeCountMapper;

    @Override
    public Map<Long, Integer> countByJobPosition() {
        return jobPositionEmployeeCountMapper.countByJobPosition().stream()
                .collect(Collectors.toMap(
                        JobPositionEmployeeCountRow::jobPositionId,
                        JobPositionEmployeeCountRow::employeeCount));
    }

    @Override
    public long countByJobPositionId(Long jobPositionId) {
        return jobPositionEmployeeCountMapper.countByJobPositionId(jobPositionId);
    }

    @Override
    public long countAllReferencing(Long jobPositionId) {
        return jobPositionEmployeeCountMapper.countAllReferencing(jobPositionId);
    }
}
