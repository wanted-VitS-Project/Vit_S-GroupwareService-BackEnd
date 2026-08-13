package com.group3.vitamins.major.application.query;

/** 전공 목록 조회 입력. keyword 공백은 null 로 눕힌다. */
public record MajorListQuery(String keyword, String role) {

    public MajorListQuery {
        keyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
    }
}
