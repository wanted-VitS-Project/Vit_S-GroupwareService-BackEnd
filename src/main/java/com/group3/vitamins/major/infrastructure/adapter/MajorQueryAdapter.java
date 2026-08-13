package com.group3.vitamins.major.infrastructure.adapter;

import com.group3.vitamins.major.application.port.MajorQueryPort;
import com.group3.vitamins.major.application.result.MajorListProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MajorQueryAdapter implements MajorQueryPort {

    private final MajorQueryMapper mapper;

    @Override
    public List<MajorListProjection> findMajorsWithCount(Long companyId, String keyword) {
        return mapper.findMajorsWithCount(companyId, keyword);
    }

    @Override
    public long countActiveReferences(Long majorId, Long companyId) {
        return mapper.countActiveReferences(majorId, companyId);
    }
}
