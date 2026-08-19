package com.group3.vitamins.major.application.service;

import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.major.application.command.CreateMajorCommand;
import com.group3.vitamins.major.application.command.DeleteMajorCommand;
import com.group3.vitamins.major.application.command.UpdateMajorCommand;
import com.group3.vitamins.major.application.port.MajorQueryPort;
import com.group3.vitamins.major.application.result.MajorResult;
import com.group3.vitamins.major.application.usecase.MajorCommandUseCase;
import com.group3.vitamins.major.domain.exception.MajorErrorCode;
import com.group3.vitamins.major.domain.model.Major;
import com.group3.vitamins.major.domain.repository.MajorRepository;
import com.group3.vitamins.qualification.application.policy.QualificationAdminPolicy;
import com.group3.vitamins.qualification.domain.model.QualificationNameRule;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 전공 마스터 쓰기 서비스 (생성·수정·삭제). 전부 ADMIN 전용, 회사스코프.
 *
 * <p>삭제는 hard delete + 참조 차단(INV-18) — 사용 사원 학력이 있으면 {@code MAJOR_IN_USE}. 이름은 회사 내 UNIQUE.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MajorCommandService implements MajorCommandUseCase {

    private final QualificationAdminPolicy adminPolicy;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;
    private final MajorRepository majorRepository;
    private final MajorQueryPort majorQueryPort;

    @Override
    public MajorResult create(CreateMajorCommand command) {
        adminPolicy.assertAdmin(command.role());
        long companyId = currentCompanyIdProvider.currentCompanyId();
        String name = validateName(command.name());

        majorRepository.findByName(name, companyId).ifPresent(m -> {
            throw new ConflictException(MajorErrorCode.MAJOR_NAME_DUPLICATED);
        });

        Major saved = majorRepository.save(Major.create(name, LocalDateTime.now(), companyId));
        return MajorResult.of(saved);
    }

    @Override
    public MajorResult update(UpdateMajorCommand command) {
        adminPolicy.assertAdmin(command.role());
        long companyId = currentCompanyIdProvider.currentCompanyId();
        String name = validateName(command.name());

        Major major = majorRepository.findById(command.majorId(), companyId)
                .orElseThrow(() -> new NotFoundException(MajorErrorCode.MAJOR_NOT_FOUND));

        // 다른 전공이 같은 이름을 쓰면 충돌. 자기 자신은 제외한다.
        majorRepository.findByName(name, companyId)
                .filter(m -> !m.getMajorId().equals(major.getMajorId()))
                .ifPresent(m -> {
                    throw new ConflictException(MajorErrorCode.MAJOR_NAME_DUPLICATED);
                });

        major.rename(name);
        return MajorResult.of(majorRepository.save(major));
    }

    @Override
    public void delete(DeleteMajorCommand command) {
        adminPolicy.assertAdmin(command.role());
        long companyId = currentCompanyIdProvider.currentCompanyId();

        Major major = majorRepository.findById(command.majorId(), companyId)
                .orElseThrow(() -> new NotFoundException(MajorErrorCode.MAJOR_NOT_FOUND));

        // 삭제 차단은 활성 사원이 아니라 전체 참조로 판정한다 — 퇴사·시스템 사원의 학력도 FK(RESTRICT)로 삭제를 막는다.
        long references = majorQueryPort.countReferences(major.getMajorId(), companyId);
        if (references > 0) {
            // 코드는 그대로, 메시지에 사원 수를 담는다(qualification.md · 부서 DEPT_HAS_EMPLOYEES 선례).
            throw new ConflictException(MajorErrorCode.MAJOR_IN_USE,
                    "사용 중인 전공은 삭제할 수 없습니다 (사원 " + references + "명).");
        }

        try {
            // 선검사~삭제 사이에 학력이 새로 달리는 경합 대비 안전망. deleteById 가 flush 하므로 FK 위반이 여기서 즉시 터진다.
            majorRepository.deleteById(major.getMajorId());
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(MajorErrorCode.MAJOR_IN_USE, e);
        }
    }

    /** 이름 규칙은 {@link QualificationNameRule} 공용 — 비어있음·100자 초과·금지 문자(`,` `;` `:` 줄바꿈)는 400. */
    private String validateName(String name) {
        if (!QualificationNameRule.isValid(name)) {
            throw new ValidationException(MajorErrorCode.MAJOR_INVALID_REQUEST);
        }
        return QualificationNameRule.normalize(name);
    }
}
