package com.group3.vitamins.project.infrastructure.adapter;

import com.group3.vitamins.project.application.port.BusinessCategoryLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BusinessCategoryLookupAdapter implements BusinessCategoryLookupPort {

    private final BusinessCategoryLookupQueryMapper businessCategoryLookupQueryMapper;

    @Override
    public List<BusinessCategoryView> findByIds(List<Long> categoryIds) {
        return businessCategoryLookupQueryMapper.findByIds(categoryIds).stream()
                .map(row -> new BusinessCategoryView(row.categoryId(), row.name(), row.code()))
                .toList();
    }
}