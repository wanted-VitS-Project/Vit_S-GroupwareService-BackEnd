package com.group3.vitamins.project.application.query;

import java.time.LocalDate;

/** 프로젝트 목록 조회 요청. 컨트롤러가 받은 원본 값이며 검증·정규화는 서비스가 한다. */
public record ProjectListQuery(
        String status,
        Long businessCategoryId,
        LocalDate startedOnFrom,
        LocalDate startedOnTo,
        String keyword,
        int page,
        int size,
        String requesterUserId,
        String role
) {
}