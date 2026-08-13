package com.group3.vitamins.major.application.result;

import com.group3.vitamins.major.domain.model.Major;

/** 전공 생성·수정 결과. */
public record MajorResult(Long majorId, String name) {

    public static MajorResult of(Major major) {
        return new MajorResult(major.getMajorId(), major.getName());
    }
}
