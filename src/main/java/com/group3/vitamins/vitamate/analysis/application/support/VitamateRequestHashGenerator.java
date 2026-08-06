package com.group3.vitamins.vitamate.analysis.application.support;

import com.group3.vitamins.global.application.support.hash.Sha256HashGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

// 비타메이트 분석 요청값을 표준 문자열로 정리해 request_hash를 생성한다
@Component
@RequiredArgsConstructor
public class VitamateRequestHashGenerator {

    private static final String DELIMITER = "|";

    private final Sha256HashGenerator sha256HashGenerator;

    // 분석 요청의 비교 기준 문자열을 만든 뒤 SHA-256 해시로 변환한다.
    public String generate(Long blockId, List<Long> fileVersionIds, String prompt) {
        String rawValue = createCanonicalValue(blockId, fileVersionIds, prompt);
        return sha256HashGenerator.generate(rawValue);
    }

    // 파일 버전 순서를 정렬해 같은 요청이면 항상 같은 원문 문자열이 나오게 만든다.
    private String createCanonicalValue(Long blockId, List<Long> fileVersionIds, String prompt) {
        String sortedFileVersionIds = fileVersionIds.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        return blockId
                + DELIMITER
                + sortedFileVersionIds
                + DELIMITER
                + prompt.trim();
    }
}
