package com.group3.vitamins.jobposition.application.service;

import com.group3.vitamins.jobposition.application.policy.JobPositionAdminPolicy;
import com.group3.vitamins.jobposition.application.port.JobPositionEmployeeCountPort;
import com.group3.vitamins.jobposition.application.query.JobPositionListQuery;
import com.group3.vitamins.jobposition.application.result.JobPositionResult;
import com.group3.vitamins.jobposition.application.usecase.JobPositionQueryUseCase;
import com.group3.vitamins.jobposition.domain.model.JobPosition;
import com.group3.vitamins.jobposition.domain.repository.JobPositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobPositionQueryService implements JobPositionQueryUseCase {

    private final JobPositionRepository jobPositionRepository;
    private final JobPositionEmployeeCountPort jobPositionEmployeeCountPort;
    private final JobPositionAdminPolicy jobPositionAdminPolicy;

    @Override
    public List<JobPositionResult> listJobPositions(JobPositionListQuery query) {
        jobPositionAdminPolicy.assertAdmin(query.role());

        List<JobPosition> jobPositions = jobPositionRepository.findAllOrdered();
        if (jobPositions.isEmpty()) {
            return List.of();
        }

        // 인원 집계를 한 번에 받아 대조한다 (항목마다 세면 N+1). 인원 0 인 직급은 맵에 없어 0 으로 채운다.
        Map<Long, Integer> countByPosition = jobPositionEmployeeCountPort.countByJobPosition();

        return jobPositions.stream()
                .map(position -> JobPositionResult.of(
                        position,
                        countByPosition.getOrDefault(position.getJobPositionId(), 0)))
                .toList();
    }
}
