
package com.group3.vitamins.businesscategory.infrastructure.persistence;

import com.group3.vitamins.businesscategory.domain.model.BusinessCategory;
import com.group3.vitamins.businesscategory.domain.repository.BusinessCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class BusinessCategoryRepositoryAdapter implements BusinessCategoryRepository {

    private final SpringDataBusinessCategoryRepository springDataRepository;

    @Override
    public List<BusinessCategory> search(String keyword, boolean includeDeleted) {
        return springDataRepository.search(keyword, includeDeleted)
                .stream()
                .map(BusinessCategoryMapper::toDomain)
                .toList();
    }
}