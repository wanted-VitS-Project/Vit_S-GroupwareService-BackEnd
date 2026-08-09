package com.group3.vitamins.vitamate.analysis.application.support;

import com.group3.vitamins.global.application.support.hash.Sha256HashGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

// 비타메이트 분석 요청값을 표준 문자열로 정리해 request_hash를 생성합니다.
@Component
@RequiredArgsConstructor
public class VitamateRequestHashGenerator {

    private static final String DELIMITER = "|";

    private final Sha256HashGenerator sha256HashGenerator;

    // 멱등성 비교에 사용할 요청 기준값을 SHA-256 해시로 변환합니다.
    public String generate(
            Long blockId,
            List<Long> referenceFileVersionIds,
            List<Long> targetFileVersionIds,
            String reviewType,
            List<String> reviewCategoryCodes,
            String prompt
    ) {
        String rawValue = createCanonicalValue(
                blockId,
                referenceFileVersionIds,
                targetFileVersionIds,
                reviewType,
                reviewCategoryCodes,
                prompt
        );
        return sha256HashGenerator.generate(rawValue);
    }

    // 순서 차이로 다른 요청처럼 보이지 않도록 파일과 카테고리 목록을 정렬합니다.
    private String createCanonicalValue(
            Long blockId,
            List<Long> referenceFileVersionIds,
            List<Long> targetFileVersionIds,
            String reviewType,
            List<String> reviewCategoryCodes,
            String prompt
    ) {
        String sortedReferenceFileVersionIds = referenceFileVersionIds.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        String sortedTargetFileVersionIds = targetFileVersionIds.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        String sortedCategoryCodes = reviewCategoryCodes.stream()
                .sorted()
                .collect(Collectors.joining(","));

        return blockId
                + DELIMITER
                + sortedReferenceFileVersionIds
                + DELIMITER
                + sortedTargetFileVersionIds
                + DELIMITER
                + reviewType.trim()
                + DELIMITER
                + sortedCategoryCodes
                + DELIMITER
                + normalize(prompt);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
