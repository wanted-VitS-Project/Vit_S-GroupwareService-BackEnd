package com.group3.vitamins.companydocument.infrastructure.adapter;

import com.group3.vitamins.companydocument.application.query.CompanyDocumentListCriteria;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentListProjection;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentVersionProjection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 사내 문서 화면용 조회 (MyBatis · 조회 전용). SQL 은 XML 에 둔다(팀 컨벤션 — @Select 금지). */
@Mapper
public interface CompanyDocumentQueryMapper {

    long countDocuments(CompanyDocumentListCriteria criteria);

    List<CompanyDocumentListProjection> findDocuments(CompanyDocumentListCriteria criteria);

    List<CompanyDocumentVersionProjection> findCompletedVersions(@Param("companyDocumentId") Long companyDocumentId);
}
