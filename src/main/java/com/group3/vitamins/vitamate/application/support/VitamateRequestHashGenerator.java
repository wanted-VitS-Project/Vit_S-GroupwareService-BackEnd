package com.group3.vitamins.vitamate.application.support;

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


    public String generate(Long blockId, List<Long> fileVersionIds, String prompt) {
        String rawValue = createCanonicalValue(blockId, fileVersionIds, prompt);
        return sha256HashGenerator.generate(rawValue);
    }

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