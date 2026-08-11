package com.group3.vitamins.project.infrastructure.adapter;

/** {@code business_category} 조회 결과 — 존재 확인 + 이름·업무코드 표시용. */
public record BusinessCategoryRow(Long categoryId, String name, String code) {
}