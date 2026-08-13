package com.group3.vitamins.major.application.usecase;

import com.group3.vitamins.major.application.query.MajorListQuery;
import com.group3.vitamins.major.application.result.MajorListItemResult;

import java.util.List;

/** 전공 마스터 조회 인바운드 포트 (목록 + 사용 사원 수). ADMIN 전용. */
public interface MajorQueryUseCase {

    List<MajorListItemResult> list(MajorListQuery query);
}
