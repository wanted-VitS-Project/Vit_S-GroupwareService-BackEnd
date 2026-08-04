package com.group3.vitamins.global.application.support.hash;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// 문자열을 SHA-256 해시 문자열로 변환하는 공통 생성기
@Component
public class Sha256HashGenerator {

    // 원문 문자열을 SHA-256으로 해싱한 뒤 16진수 문자열로 반환한다.
    public String generate(String rawValue) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawValue.getBytes(StandardCharsets.UTF_8));
            return toHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", e);
        }
    }

    // 해시 바이트 배열을 저장 가능한 16진수 문자열로 바꾼다.
    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);

        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }

        return builder.toString();
    }
}
