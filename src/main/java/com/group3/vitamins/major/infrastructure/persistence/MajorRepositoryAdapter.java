package com.group3.vitamins.major.infrastructure.persistence;

import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.major.domain.exception.MajorErrorCode;
import com.group3.vitamins.major.domain.model.Major;
import com.group3.vitamins.major.domain.repository.MajorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MajorRepositoryAdapter implements MajorRepository {

    private final SpringDataMajorRepository springDataRepository;

    @Override
    public Major save(Major major) {
        try {
            // saveAndFlush — UNIQUE(company_id, name) 위반을 커밋이 아니라 쓰기 시점에 발생시킨다.
            return MajorPersistenceMapper.toDomain(
                    springDataRepository.saveAndFlush(MajorPersistenceMapper.toEntity(major)));
        } catch (DataIntegrityViolationException e) {
            // 앱 선검사와 저장 사이의 경합 — 이름 중복을 도메인 코드로 변환한다.
            throw new ConflictException(MajorErrorCode.MAJOR_NAME_DUPLICATED, e);
        }
    }

    @Override
    public Optional<Major> findById(Long majorId, Long companyId) {
        return springDataRepository.findByMajorIdAndCompanyId(majorId, companyId)
                .map(MajorPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Major> findByName(String name, Long companyId) {
        return springDataRepository.findByCompanyIdAndName(companyId, name)
                .map(MajorPersistenceMapper::toDomain);
    }

    @Override
    public void deleteById(Long majorId) {
        springDataRepository.deleteById(majorId);
        springDataRepository.flush();
    }
}
