package com.group3.vitamins.jobposition.infrastructure.persistence;

import com.group3.vitamins.jobposition.domain.model.JobPosition;
import com.group3.vitamins.jobposition.domain.repository.JobPositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JobPositionRepositoryAdapter implements JobPositionRepository {

    private final SpringDataJobPositionRepository springDataRepository;

    @Override
    public List<JobPosition> findAllOrdered(Long companyId) {
        return springDataRepository.findAllByCompanyIdOrderBySortOrderAscNameAsc(companyId)
                .stream()
                .map(JobPositionMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<JobPosition> findByName(String name, Long companyId) {
        return springDataRepository.findByNameAndCompanyId(name, companyId)
                .map(JobPositionMapper::toDomain);
    }

    @Override
    public Optional<JobPosition> findById(Long jobPositionId, Long companyId) {
        return springDataRepository.findByJobPositionIdAndCompanyId(jobPositionId, companyId)
                .map(JobPositionMapper::toDomain);
    }

    @Override
    public int nextSortOrder(Long companyId) {
        Integer max = springDataRepository.findMaxSortOrder(companyId);
        return (max == null ? 0 : max) + 1;
    }

    @Override
    public JobPosition save(JobPosition jobPosition) {
        // saveAndFlush — 이름 UNIQUE 위반을 커밋이 아니라 지금 발생시킨다.
        // 일반 save 는 UPDATE(수정 경로)를 커밋 때 flush 해서, 서비스의 try/catch 밖에서 예외가 터진다.
        return JobPositionMapper.toDomain(
                springDataRepository.saveAndFlush(JobPositionMapper.toEntity(jobPosition)));
    }

    @Override
    public void delete(JobPosition jobPosition) {
        springDataRepository.deleteById(jobPosition.getJobPositionId());
        // flush — DELETE 를 지금 실행해 FK 위반을 커밋이 아니라 이 시점에 발생시킨다.
        // 그래야 서비스의 try/catch 가 잡아 POS_IN_USE(409)로 변환할 수 있다(안 하면 커밋 때 터져 500).
        springDataRepository.flush();
    }
}
