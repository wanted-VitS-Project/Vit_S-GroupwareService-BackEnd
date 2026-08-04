package com.group3.vitamins.businesscategory.application.service;

import com.group3.vitamins.businesscategory.application.policy.BusinessCategoryAdminPolicy;
import com.group3.vitamins.businesscategory.application.port.ProjectCategoryLinkPort;
import com.group3.vitamins.businesscategory.application.query.BusinessCategoryListQuery;
import com.group3.vitamins.businesscategory.application.result.BusinessCategoryResult;
import com.group3.vitamins.businesscategory.application.usecase.BusinessCategoryQueryUseCase;
import com.group3.vitamins.businesscategory.domain.model.BusinessCategory;
import com.group3.vitamins.businesscategory.domain.repository.BusinessCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessCategoryQueryService implements BusinessCategoryQueryUseCase {

    private final BusinessCategoryRepository businessCategoryRepository;
    private final ProjectCategoryLinkPort projectCategoryLinkPort;
    private final BusinessCategoryAdminPolicy businessCategoryAdminPolicy;

    @Override
    public List<BusinessCategoryResult> listCategories(BusinessCategoryListQuery query) {
        if (query.includeDeleted()) {
            businessCategoryAdminPolicy.assertAdmin(query.role());
        }

        List<BusinessCategory> categories =
                businessCategoryRepository.search(query.keyword(), query.includeDeleted());
        if (categories.isEmpty()) {
            return List.of();
        }

        Set<Long> linkedCategoryIds = projectCategoryLinkPort.findLinkedCategoryIds();

        return categories.stream()
                .map(category -> BusinessCategoryResult.of(
                        category, !linkedCategoryIds.contains(category.getBusinessCategoryId())))
                .toList();
    }
}