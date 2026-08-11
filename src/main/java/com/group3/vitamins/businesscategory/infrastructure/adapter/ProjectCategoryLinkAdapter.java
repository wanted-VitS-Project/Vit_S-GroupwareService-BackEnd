package com.group3.vitamins.businesscategory.infrastructure.adapter;

import com.group3.vitamins.businesscategory.application.port.ProjectCategoryLinkPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ProjectCategoryLinkAdapter implements ProjectCategoryLinkPort {

    private final ProjectCategoryLinkQueryMapper projectCategoryLinkQueryMapper;

    @Override
    public Set<Long> findLinkedCategoryIds(Long companyId) {
        return new HashSet<>(projectCategoryLinkQueryMapper.findLinkedCategoryIds(companyId));
    }

    @Override
    public long countLinkedProjects(Long categoryId, Long companyId) {
        return projectCategoryLinkQueryMapper.countLinkedProjects(categoryId, companyId);
    }
}