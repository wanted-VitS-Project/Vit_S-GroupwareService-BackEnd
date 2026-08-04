package com.group3.vitamins.project.domain.repository;

import com.group3.vitamins.project.domain.model.ProjectMember;

public interface ProjectMemberRepository {
    ProjectMember save(ProjectMember member);
}