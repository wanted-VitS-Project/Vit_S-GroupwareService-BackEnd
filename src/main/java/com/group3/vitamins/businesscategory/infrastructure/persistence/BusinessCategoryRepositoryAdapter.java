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
    public List<BusinessCategory> search(String keyword, boolean includeDeleted) {
        return springDataRepository.search(keyword, includeDeleted)
                .stream()
                .map(BusinessCategoryMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<BusinessCategory> findByName(String name) {
        return springDataRepository.findByName(name)
                .map(BusinessCategoryMapper::toDomain);
    }

    @Override
    public Optional<BusinessCategory> findByCode(String code) {
        return springDataRepository.findByCode(code)
                .map(BusinessCategoryMapper::toDomain);
    }

    @Override
    public BusinessCategory save(BusinessCategory category) {
        return BusinessCategoryMapper.toDomain(
                springDataRepository.save(BusinessCategoryMapper.toEntity(category)));
    }

    @Override
    public Optional<BusinessCategory> findActiveById(Long categoryId) {
        return springDataRepository.findByBusinessCategoryIdAndDeletedAtIsNull(categoryId)
                .map(BusinessCategoryMapper::toDomain);
    }
}