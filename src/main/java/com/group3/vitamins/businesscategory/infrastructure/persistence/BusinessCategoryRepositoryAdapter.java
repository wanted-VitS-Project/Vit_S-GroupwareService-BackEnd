package com.group3.vitamins.businesscategory.infrastructure.persistence;

import com.group3.vitamins.businesscategory.domain.model.BusinessCategory;
import com.group3.vitamins.businesscategory.domain.repository.BusinessCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BusinessCategoryRepositoryAdapter implements BusinessCategoryRepository {

    private final SpringDataBusinessCategoryRepository springDataRepository;

    @Override
    public List<BusinessCategory> search(String keyword, boolean includeDeleted, Long companyId) {
        return springDataRepository.search(keyword, includeDeleted, companyId)
                .stream()
                .map(BusinessCategoryMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<BusinessCategory> findActiveByName(String name, Long companyId) {
        return springDataRepository.findByNameAndCompanyIdAndDeletedAtIsNull(name, companyId)
                .map(BusinessCategoryMapper::toDomain);
    }

    @Override
    public Optional<BusinessCategory> findActiveByCode(String code, Long companyId) {
        return springDataRepository.findByCodeAndCompanyIdAndDeletedAtIsNull(code, companyId)
                .map(BusinessCategoryMapper::toDomain);
    }

    @Override
    public BusinessCategory save(BusinessCategory category) {
        return BusinessCategoryMapper.toDomain(
                springDataRepository.save(BusinessCategoryMapper.toEntity(category)));
    }

    @Override
    public Optional<BusinessCategory> findActiveById(Long categoryId, Long companyId) {
        return springDataRepository
                .findByBusinessCategoryIdAndCompanyIdAndDeletedAtIsNull(categoryId, companyId)
                .map(BusinessCategoryMapper::toDomain);
    }
}
