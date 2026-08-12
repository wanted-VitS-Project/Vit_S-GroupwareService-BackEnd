package com.group3.vitamins.businesscategory.application.service;

import com.group3.vitamins.businesscategory.application.command.CreateBusinessCategoryCommand;
import com.group3.vitamins.businesscategory.application.command.UpdateBusinessCategoryCommand;
import com.group3.vitamins.businesscategory.application.policy.BusinessCategoryAdminPolicy;
import com.group3.vitamins.businesscategory.application.port.ProjectCategoryLinkPort;
import com.group3.vitamins.businesscategory.application.result.BusinessCategoryResult;
import com.group3.vitamins.businesscategory.application.usecase.BusinessCategoryCommandUseCase;
import com.group3.vitamins.businesscategory.domain.exception.BusinessCategoryErrorCode;
import com.group3.vitamins.businesscategory.domain.model.BusinessCategory;
import com.group3.vitamins.businesscategory.domain.repository.BusinessCategoryRepository;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import com.group3.vitamins.businesscategory.application.command.DeleteBusinessCategoryCommand;

@Service
@RequiredArgsConstructor
@Transactional
public class BusinessCategoryCommandService implements BusinessCategoryCommandUseCase {

    private static final int NAME_MAX_LENGTH = 100;
    private static final int CODE_MAX_LENGTH = 30;

    private final BusinessCategoryRepository businessCategoryRepository;
    private final ProjectCategoryLinkPort projectCategoryLinkPort;
    private final BusinessCategoryAdminPolicy businessCategoryAdminPolicy;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;

    @Override
    public BusinessCategoryResult createCategory(CreateBusinessCategoryCommand command) {
        businessCategoryAdminPolicy.assertAdmin(command.role());
        validateName(command.name());
        validateCode(command.code());

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        checkNameDuplicate(command.name(), null, companyId);
        if (command.code() != null) {
            checkCodeDuplicate(command.code(), null, companyId);
        }

        BusinessCategory saved = saveHandlingUniqueConflict(
                BusinessCategory.create(command.name(), command.code(), command.description(),
                        LocalDateTime.now(), companyId));

        return BusinessCategoryResult.of(saved, true);
    }

    @Override
    public BusinessCategoryResult updateCategory(UpdateBusinessCategoryCommand command) {
        businessCategoryAdminPolicy.assertAdmin(command.role());

        if (!command.nameProvided() && !command.codeProvided() && !command.descriptionProvided()) {
            throw new ValidationException(BusinessCategoryErrorCode.BUSINESS_CATEGORY_NO_FIELD_TO_UPDATE);
        }

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        BusinessCategory category = businessCategoryRepository
                .findActiveById(command.categoryId(), companyId)
                .orElseThrow(() -> new NotFoundException(BusinessCategoryErrorCode.BUSINESS_CATEGORY_NOT_FOUND));

        if (command.nameProvided()) {
            validateName(command.name());
            checkNameDuplicate(command.name(), category.getBusinessCategoryId(), companyId);
            category.rename(command.name());
        }
        if (command.codeProvided()) {
            validateCode(command.code());
            if (command.code() != null) {
                checkCodeDuplicate(command.code(), category.getBusinessCategoryId(), companyId);
            }
            category.changeCode(command.code());
        }
        if (command.descriptionProvided()) {
            category.changeDescription(command.description());
        }

        BusinessCategory saved = saveHandlingUniqueConflict(category);
        boolean deletable = !projectCategoryLinkPort.findLinkedCategoryIds(companyId)
                .contains(saved.getBusinessCategoryId());

        return BusinessCategoryResult.of(saved, deletable);
    }

    @Override
    public void deleteCategory(DeleteBusinessCategoryCommand command) {
        businessCategoryAdminPolicy.assertAdmin(command.role());

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        BusinessCategory category = businessCategoryRepository
                .findActiveById(command.categoryId(), companyId)
                .orElseThrow(() -> new NotFoundException(BusinessCategoryErrorCode.BUSINESS_CATEGORY_NOT_FOUND));

        long linkedCount = projectCategoryLinkPort.countLinkedProjects(
                category.getBusinessCategoryId(), companyId);
        if (linkedCount > 0) {
            throw new ConflictException(BusinessCategoryErrorCode.BUSINESS_CATEGORY_IN_USE,
                    "사용 중인 카테고리는 삭제할 수 없습니다. (프로젝트 " + linkedCount + "건)");
        }

        category.delete(LocalDateTime.now());
        businessCategoryRepository.save(category);
    }

    /** 이름 형식을 검증한다. null·공백·100자 초과를 막는다. */
    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException(BusinessCategoryErrorCode.BUSINESS_CATEGORY_NAME_REQUIRED);
        }
        if (name.length() > NAME_MAX_LENGTH) {
            throw new ValidationException(BusinessCategoryErrorCode.BUSINESS_CATEGORY_FIELD_TOO_LONG);
        }
    }

    /** 업무코드 형식을 검증한다. null 은 허용(코드 없음/지움), 빈 문자열과 30자 초과만 막는다. */
    private void validateCode(String code) {
        if (code == null) {
            return;
        }
        if (code.isEmpty()) {
            throw new ValidationException(BusinessCategoryErrorCode.BUSINESS_CATEGORY_CODE_INVALID);
        }
        if (code.length() > CODE_MAX_LENGTH) {
            throw new ValidationException(BusinessCategoryErrorCode.BUSINESS_CATEGORY_FIELD_TOO_LONG);
        }
    }

    /**
     * 이름 중복을 검사한다. excludeId 는 수정 중인 자기 자신 — 있으면 건너뛴다.
     * 활성 행만 대상으로 한다 (DELETE.md §6-1) — 삭제된 이름은 재사용을 막지 않는다.
     */
    private void checkNameDuplicate(String name, Long excludeId, Long companyId) {
        businessCategoryRepository.findActiveByName(name, companyId)
                .filter(existing -> !existing.getBusinessCategoryId().equals(excludeId))
                .ifPresent(existing -> {
                    throw new ConflictException(BusinessCategoryErrorCode.BUSINESS_CATEGORY_NAME_DUPLICATED);
                });
    }

    /**
     * 업무코드 중복을 검사한다. excludeId 는 수정 중인 자기 자신 — 있으면 건너뛴다.
     * 활성 행만 대상으로 한다 (DELETE.md §6-1) — 삭제된 업무코드는 재사용을 막지 않는다.
     */
    private void checkCodeDuplicate(String code, Long excludeId, Long companyId) {
        businessCategoryRepository.findActiveByCode(code, companyId)
                .filter(existing -> !existing.getBusinessCategoryId().equals(excludeId))
                .ifPresent(existing -> {
                    throw new ConflictException(BusinessCategoryErrorCode.BUSINESS_CATEGORY_CODE_DUPLICATED);
                });
    }

    /**
     * 저장 중 활성 UNIQUE(uk_bc_active_name · uk_bc_active_code) 위반을 409 로 변환한다.
     *
     * <p>위 check* 로 활성 행을 선검사하지만, 검사와 저장 사이에 동시 요청이 겹칠 수 있다.
     * DB UNIQUE 가 최종 방어선이고, 그 위반을 전역 핸들러가 500 으로 흘리지 않도록 여기서 409 로 바꾼다
     * (department·job_position 선례). 어느 제약인지에 따라 이름/업무코드 에러를 구분한다.
     */
    private BusinessCategory saveHandlingUniqueConflict(BusinessCategory category) {
        try {
            return businessCategoryRepository.save(category);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(resolveUniqueViolation(e), e);
        }
    }

    /** UNIQUE 위반 예외의 제약명으로 이름/업무코드 에러코드를 판정한다. 코드 제약이 아니면 이름 충돌로 본다. */
    private BusinessCategoryErrorCode resolveUniqueViolation(DataIntegrityViolationException e) {
        String cause = e.getMostSpecificCause().getMessage();
        if (cause != null && cause.contains("uk_bc_active_code")) {
            return BusinessCategoryErrorCode.BUSINESS_CATEGORY_CODE_DUPLICATED;
        }
        return BusinessCategoryErrorCode.BUSINESS_CATEGORY_NAME_DUPLICATED;
    }
}