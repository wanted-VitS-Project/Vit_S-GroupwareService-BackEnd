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
    public List<BusinessCategoryView> findByIds(List<Long> categoryIds, Long companyId) {
        return businessCategoryLookupQueryMapper.findByIds(categoryIds, companyId).stream()
                .map(row -> new BusinessCategoryView(row.categoryId(), row.name(), row.code()))
                .toList();
    }

    /** 삭제된 카테고리도 그대로 담는다 — 걸러내면 이미 연결된 것이 응답에서 사라진다. */
    @Override
    public List<BusinessCategoryRef> findRefsByIds(List<Long> categoryIds, Long companyId) {
        return businessCategoryLookupQueryMapper.findRefsByIds(categoryIds, companyId).stream()
                .map(row -> new BusinessCategoryRef(
                        row.categoryId(), row.name(), row.code(), row.deleted()))
                .toList();
    }
}