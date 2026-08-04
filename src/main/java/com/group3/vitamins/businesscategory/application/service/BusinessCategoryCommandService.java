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
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class BusinessCategoryCommandService implements BusinessCategoryCommandUseCase {

    private static final int NAME_MAX_LENGTH = 100;
    private static final int CODE_MAX_LENGTH = 30;

    private final BusinessCategoryRepository businessCategoryRepository;
    private final ProjectCategoryLinkPort projectCategoryLinkPort;
    private final BusinessCategoryAdminPolicy businessCategoryAdminPolicy;

    @Override
    public BusinessCategoryResult createCategory(CreateBusinessCategoryCommand command) {
        businessCategoryAdminPolicy.assertAdmin(command.role());
        validateName(command.name());
        validateCode(command.code());
        checkNameDuplicate(command.name(), null);
        if (command.code() != null) {
            checkCodeDuplicate(command.code(), null);
        }

        BusinessCategory saved = businessCategoryRepository.save(
                BusinessCategory.create(command.name(), command.code(), command.description(),
                        LocalDateTime.now()));

        return BusinessCategoryResult.of(saved, true);
    }

    @Override
    public BusinessCategoryResult updateCategory(UpdateBusinessCategoryCommand command) {
        businessCategoryAdminPolicy.assertAdmin(command.role());

        if (!command.nameProvided() && !command.codeProvided() && !command.descriptionProvided()) {
            throw new ValidationException(BusinessCategoryErrorCode.BUSINESS_CATEGORY_NO_FIELD_TO_UPDATE);
        }

        BusinessCategory category = businessCategoryRepository.findActiveById(command.categoryId())
                .orElseThrow(() -> new NotFoundException(BusinessCategoryErrorCode.BUSINESS_CATEGORY_NOT_FOUND));

        if (command.nameProvided()) {
            validateName(command.name());
            checkNameDuplicate(command.name(), category.getBusinessCategoryId());
            category.rename(command.name());
        }
        if (command.codeProvided()) {
            validateCode(command.code());
            if (command.code() != null) {
                checkCodeDuplicate(command.code(), category.getBusinessCategoryId());
            }
            category.changeCode(command.code());
        }
        if (command.descriptionProvided()) {
            category.changeDescription(command.description());
        }

        BusinessCategory saved = businessCategoryRepository.save(category);
        boolean deletable = !projectCategoryLinkPort.findLinkedCategoryIds().contains(saved.getBusinessCategoryId());

        return BusinessCategoryResult.of(saved, deletable);
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

    /** 이름 중복을 검사한다. excludeId 는 수정 중인 자기 자신 — 있으면 건너뛴다. 삭제분에 걸리면 안내 문구를 달리 낸다 (§3-1). */
    private void checkNameDuplicate(String name, Long excludeId) {
        businessCategoryRepository.findByName(name)
                .filter(existing -> !existing.getBusinessCategoryId().equals(excludeId))
                .ifPresent(existing -> {
                    BusinessCategoryErrorCode errorCode = BusinessCategoryErrorCode.BUSINESS_CATEGORY_NAME_DUPLICATED;
                    if (existing.isDeleted()) {
                        throw new ConflictException(errorCode, "삭제된 카테고리에 같은 이름이 있습니다. 다른 이름을 쓰세요.");
                    }
                    throw new ConflictException(errorCode);
                });
    }

    /** 업무코드 중복을 검사한다. excludeId 는 수정 중인 자기 자신 — 있으면 건너뛴다. 삭제분에 걸리면 안내 문구를 달리 낸다 (§3-1). */
    private void checkCodeDuplicate(String code, Long excludeId) {
        businessCategoryRepository.findByCode(code)
                .filter(existing -> !existing.getBusinessCategoryId().equals(excludeId))
                .ifPresent(existing -> {
                    BusinessCategoryErrorCode errorCode = BusinessCategoryErrorCode.BUSINESS_CATEGORY_CODE_DUPLICATED;
                    if (existing.isDeleted()) {
                        throw new ConflictException(errorCode, "삭제된 카테고리에 같은 업무코드가 있습니다. 다른 코드를 쓰세요.");
                    }
                    throw new ConflictException(errorCode);
                });
    }
}