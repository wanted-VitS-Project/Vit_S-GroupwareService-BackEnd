package com.group3.vitamins.major.application.service;

import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.major.application.query.MajorListQuery;
import com.group3.vitamins.major.application.result.MajorListItemResult;
import com.group3.vitamins.major.application.port.MajorQueryPort;
import com.group3.vitamins.major.application.usecase.MajorQueryUseCase;
import com.group3.vitamins.qualification.application.policy.QualificationAdminPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 전공 마스터 조회 서비스 (목록 + 사용 사원 수). 읽기 전용, ADMIN 전용. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MajorQueryService implements MajorQueryUseCase {

    private final QualificationAdminPolicy adminPolicy;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;
    private final MajorQueryPort majorQueryPort;

    @Override
    public List<MajorListItemResult> list(MajorListQuery query) {
        adminPolicy.assertAdmin(query.role());
        long companyId = currentCompanyIdProvider.currentCompanyId();
        return majorQueryPort.findMajorsWithCount(companyId, query.keyword()).stream()
                .map(MajorListItemResult::from)
                .toList();
    }
}
