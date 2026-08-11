package com.group3.vitamins.project.application.usecase;

import com.group3.vitamins.project.application.query.MemberListQuery;
import com.group3.vitamins.project.application.result.MemberSummary;

import java.util.List;

public interface ProjectMemberQueryUseCase {

    List<MemberSummary> getMembers(MemberListQuery query);
}