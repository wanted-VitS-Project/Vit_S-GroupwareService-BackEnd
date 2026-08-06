package com.group3.vitamins.jobposition.infrastructure.adapter;

import com.group3.vitamins.jobposition.application.port.JobPositionEmployeeQueryPort;
import com.group3.vitamins.jobposition.application.result.JobPositionEmployeeRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@link JobPositionEmployeeQueryPort} 의 MyBatis 어댑터. 실제 SQL 은
 * {@link JobPositionEmployeeQueryMapper} 와 그 XML 이 갖는다.
 */
@Component
@RequiredArgsConstructor
public class JobPositionEmployeeQueryAdapter implements JobPositionEmployeeQueryPort {

    private final JobPositionEmployeeQueryMapper jobPositionEmployeeQueryMapper;

    @Override
    public List<JobPositionEmployeeRow> findEmployeesByJobPosition(Long jobPositionId) {
        return jobPositionEmployeeQueryMapper.findEmployeesByJobPosition(jobPositionId);
    }
}
