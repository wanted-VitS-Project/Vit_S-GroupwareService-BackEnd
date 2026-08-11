package com.group3.vitamins.bidding.bidnotice.application.support;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;

// 직접 등록 공고의 회사별 중복 판정에 사용할 안정적인 SHA-256 키를 생성합니다.
@Component
public class ManualBidNoticeDedupKeyGenerator {

    // 중복 기준 네 필드를 같은 규칙으로 정규화한 뒤 고정 길이 키로 변환합니다.
    public String generate(
            String noticeName,
            String noticeAgency,
            LocalDateTime announcedAt,
            LocalDateTime bidDeadlineAt
    ) {
        String canonical = encode(normalize(noticeName))
                + encode(normalize(noticeAgency))
                + encode(announcedAt.toString())
                + encode(bidDeadlineAt.toString());

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(canonical.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }

    // 공백과 대소문자 차이로 같은 공고가 다른 키를 갖지 않도록 통일합니다.
    private String normalize(String value) {
        return value.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    // 값의 길이와 본문을 함께 기록해 필드 안 구분자가 중복 키 경계를 흐리지 않게 합니다.
    private String encode(String value) {
        return value.length() + ":" + value;
    }
}
