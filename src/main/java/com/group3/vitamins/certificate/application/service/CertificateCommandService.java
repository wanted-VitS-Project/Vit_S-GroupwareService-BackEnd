package com.group3.vitamins.certificate.application.service;

import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.certificate.application.command.CreateCertificateCommand;
import com.group3.vitamins.certificate.application.command.DeleteCertificateCommand;
import com.group3.vitamins.certificate.application.command.UpdateCertificateCommand;
import com.group3.vitamins.certificate.application.port.CertificateQueryPort;
import com.group3.vitamins.certificate.application.result.CertificateResult;
import com.group3.vitamins.certificate.application.usecase.CertificateCommandUseCase;
import com.group3.vitamins.certificate.domain.exception.CertificateErrorCode;
import com.group3.vitamins.certificate.domain.model.Certificate;
import com.group3.vitamins.certificate.domain.repository.CertificateRepository;
import com.group3.vitamins.qualification.application.policy.QualificationAdminPolicy;
import com.group3.vitamins.qualification.domain.model.QualificationNameRule;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 자격증 마스터 쓰기 서비스 (생성·수정·삭제). 전부 ADMIN 전용, 회사스코프.
 *
 * <p>삭제는 hard delete + 참조 차단(INV-18) — 사용 사원 학력이 있으면 {@code CERT_IN_USE}. 이름은 회사 내 UNIQUE.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CertificateCommandService implements CertificateCommandUseCase {

    private final QualificationAdminPolicy adminPolicy;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;
    private final CertificateRepository certificateRepository;
    private final CertificateQueryPort certificateQueryPort;

    @Override
    public CertificateResult create(CreateCertificateCommand command) {
        adminPolicy.assertAdmin(command.role());
        long companyId = currentCompanyIdProvider.currentCompanyId();
        String name = validateName(command.name());

        certificateRepository.findByName(name, companyId).ifPresent(m -> {
            throw new ConflictException(CertificateErrorCode.CERT_NAME_DUPLICATED);
        });

        Certificate saved = certificateRepository.save(Certificate.create(name, LocalDateTime.now(), companyId));
        return CertificateResult.of(saved);
    }

    @Override
    public CertificateResult update(UpdateCertificateCommand command) {
        adminPolicy.assertAdmin(command.role());
        long companyId = currentCompanyIdProvider.currentCompanyId();
        String name = validateName(command.name());

        Certificate certificate = certificateRepository.findById(command.certificateId(), companyId)
                .orElseThrow(() -> new NotFoundException(CertificateErrorCode.CERT_NOT_FOUND));

        // 다른 자격증이 같은 이름을 쓰면 충돌. 자기 자신은 제외한다.
        certificateRepository.findByName(name, companyId)
                .filter(m -> !m.getCertificateId().equals(certificate.getCertificateId()))
                .ifPresent(m -> {
                    throw new ConflictException(CertificateErrorCode.CERT_NAME_DUPLICATED);
                });

        certificate.rename(name);
        return CertificateResult.of(certificateRepository.save(certificate));
    }

    @Override
    public void delete(DeleteCertificateCommand command) {
        adminPolicy.assertAdmin(command.role());
        long companyId = currentCompanyIdProvider.currentCompanyId();

        Certificate certificate = certificateRepository.findById(command.certificateId(), companyId)
                .orElseThrow(() -> new NotFoundException(CertificateErrorCode.CERT_NOT_FOUND));

        // 삭제 차단은 활성 사원이 아니라 전체 참조로 판정한다 — 퇴사·시스템 사원의 자격증도 FK(RESTRICT)로 삭제를 막는다.
        long references = certificateQueryPort.countReferences(certificate.getCertificateId(), companyId);
        if (references > 0) {
            // 코드는 그대로, 메시지에 사원 수를 담는다(qualification.md · 부서 DEPT_HAS_EMPLOYEES 선례).
            throw new ConflictException(CertificateErrorCode.CERT_IN_USE,
                    "사용 중인 자격증은 삭제할 수 없습니다 (사원 " + references + "명).");
        }

        try {
            // 선검사~삭제 사이 경합 대비 안전망. deleteById 가 flush 하므로 FK 위반이 여기서 즉시 터진다.
            certificateRepository.deleteById(certificate.getCertificateId());
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(CertificateErrorCode.CERT_IN_USE, e);
        }
    }

    /** 이름 규칙은 {@link QualificationNameRule} 공용 — 비어있음·100자 초과·금지 문자(`,` `;` `:` 줄바꿈)는 400. */
    private String validateName(String name) {
        if (!QualificationNameRule.isValid(name)) {
            throw new ValidationException(CertificateErrorCode.CERT_INVALID_REQUEST);
        }
        return QualificationNameRule.normalize(name);
    }
}
