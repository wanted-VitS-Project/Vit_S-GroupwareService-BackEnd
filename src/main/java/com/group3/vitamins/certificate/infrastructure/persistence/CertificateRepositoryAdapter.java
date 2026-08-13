package com.group3.vitamins.certificate.infrastructure.persistence;

import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.certificate.domain.exception.CertificateErrorCode;
import com.group3.vitamins.certificate.domain.model.Certificate;
import com.group3.vitamins.certificate.domain.repository.CertificateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CertificateRepositoryAdapter implements CertificateRepository {

    private final SpringDataCertificateRepository springDataRepository;

    @Override
    public Certificate save(Certificate certificate) {
        try {
            // saveAndFlush — UNIQUE(company_id, name) 위반을 커밋이 아니라 쓰기 시점에 발생시킨다.
            return CertificatePersistenceMapper.toDomain(
                    springDataRepository.saveAndFlush(CertificatePersistenceMapper.toEntity(certificate)));
        } catch (DataIntegrityViolationException e) {
            // 앱 선검사와 저장 사이의 경합 — 이름 중복을 도메인 코드로 변환한다.
            throw new ConflictException(CertificateErrorCode.CERT_NAME_DUPLICATED, e);
        }
    }

    @Override
    public Optional<Certificate> findById(Long certificateId, Long companyId) {
        return springDataRepository.findByCertificateIdAndCompanyId(certificateId, companyId)
                .map(CertificatePersistenceMapper::toDomain);
    }

    @Override
    public Optional<Certificate> findByName(String name, Long companyId) {
        return springDataRepository.findByCompanyIdAndName(companyId, name)
                .map(CertificatePersistenceMapper::toDomain);
    }

    @Override
    public void deleteById(Long certificateId) {
        springDataRepository.deleteById(certificateId);
        springDataRepository.flush();
    }
}
