package com.group3.vitamins.certificate.infrastructure.adapter;

import com.group3.vitamins.certificate.application.result.CertificateListProjection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 자격증 마스터 화면용 조회 (MyBatis · 조회 전용). SQL 은 XML 에 둔다(팀 컨벤션). */
@Mapper
public interface CertificateQueryMapper {

    List<CertificateListProjection> findCertificatesWithCount(@Param("companyId") Long companyId, @Param("keyword") String keyword);

    long countActiveReferences(@Param("certificateId") Long certificateId, @Param("companyId") Long companyId);
}
