package com.group3.vitamins.project.block.application.result;

/** 이슈 등록 페이지의 「관련 블록 연결」 선택 후보. 상세·집계·담당자를 담지 않는다. */
public record BlockOption(
        Long blockId,
        String type,
        String title
) {
}