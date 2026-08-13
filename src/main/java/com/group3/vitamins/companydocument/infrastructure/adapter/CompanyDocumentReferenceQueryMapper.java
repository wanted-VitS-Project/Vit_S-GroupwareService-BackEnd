package com.group3.vitamins.companydocument.infrastructure.adapter;

import com.group3.vitamins.companydocument.application.result.CompanyDocumentReferenceView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 참조 선택용 사내 문서 조회 MyBatis 매퍼. SQL 은
 * {@code src/main/resources/mapper/companydocument/CompanyDocumentReferenceQueryMapper.xml} 에 둔다(팀 컨벤션).
 */
@Mapper
public interface CompanyDocumentReferenceQueryMapper {

    List<CompanyDocumentReferenceView> findSelectableDocuments(
            @Param("companyId") Long companyId, @Param("category") String category, @Param("keyword") String keyword);

    Optional<CompanyDocumentReferenceView> findSelectableVersion(
            @Param("versionId") Long versionId, @Param("companyId") Long companyId);
}
