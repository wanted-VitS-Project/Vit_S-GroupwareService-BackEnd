package com.group3.vitamins.bidding.projectconversion.application.result;

// 뼈대 단계 - projectId만 반환한다. 검증 로직·응답 필드 확장은 다음 단계에서 채운다.
public record ConvertNoticeToProjectResult(
        Long projectId
) {
}
