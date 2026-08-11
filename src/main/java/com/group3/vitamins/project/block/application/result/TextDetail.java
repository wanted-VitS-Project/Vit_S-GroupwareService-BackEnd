package com.group3.vitamins.project.block.application.result;

/** TEXT 블록 상세. content 는 마크다운이며 추가 직후에는 null 이다. version은 PATCH 낙관락 기대값(CONCURRENCY.md). */
public record TextDetail(Long txtId, String content, int version) implements BlockDetail {
}