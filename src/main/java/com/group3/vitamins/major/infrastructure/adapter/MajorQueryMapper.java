package com.group3.vitamins.major.infrastructure.adapter;

import com.group3.vitamins.major.application.result.MajorListProjection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 전공 마스터 화면용 조회 (MyBatis · 조회 전용). SQL 은 XML 에 둔다(팀 컨벤션). */
@Mapper
public interface MajorQueryMapper {

    List<MajorListProjection> findMajorsWithCount(@Param("companyId") Long companyId, @Param("keyword") String keyword);

    long countActiveReferences(@Param("majorId") Long majorId, @Param("companyId") Long companyId);
}
