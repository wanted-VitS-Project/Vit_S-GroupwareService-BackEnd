package com.group3.vitamins.settlement.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * 출금 정산 계좌번호를 저장 전 암호화한다 (DDL 주석 "암호화해서 저장").
 *
 * <p>AES/GCM — IV(12바이트)를 매 호출 새로 뽑아 암호문 앞에 붙여 Base64 로 인코딩한다.
 * PATCH 응답의 마스킹은 요청으로 받은 평문을 그대로 마스킹한 것이지 저장값을 복호화해서
 * 만들지 않는다. {@link #decrypt} 는 블록 목록 조회(마이바티스 배치 조회)와 활동 로그의
 * "변경 전" 값을 마스킹하기 위한 용도로만 쓴다 — 복호화한 원문을 그대로 응답/로그에 내보내지 않는다.
 *
 * <p>키는 {@code SETTLEMENT_ACCOUNT_ENC_KEY} 환경변수(Base64 인코딩된 AES-256 32바이트 키)로 주입한다.
 * ⚠️ PUBLIC 레포 — 실제 키 값은 여기에도 application-local.yml 에도 커밋하지 않는다 (.ai/AGENTS.md §6).
 */
@Component
public class AccountNumberCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final int AES_256_KEY_LENGTH_BYTES = 32;

    private final SecretKeySpec secretKey;

    public AccountNumberCipher(@Value("${settlement.account-number.encryption-key}") String base64Key) {
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(base64Key);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("SETTLEMENT_ACCOUNT_ENC_KEY 가 올바른 Base64 값이 아닙니다.", e);
        }
        if (decoded.length != AES_256_KEY_LENGTH_BYTES) {
            throw new IllegalStateException(
                    "SETTLEMENT_ACCOUNT_ENC_KEY 는 Base64 디코드 후 32바이트(AES-256)여야 합니다. 실제 길이: "
                            + decoded.length + "바이트");
        }
        this.secretKey = new SecretKeySpec(decoded, "AES");
    }

    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            SecureRandom.getInstanceStrong().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
            buffer.put(iv).put(cipherText);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("계좌번호 암호화 실패", e);
        }
    }

    public String decrypt(String base64CipherText) {
        try {
            byte[] combined = Base64.getDecoder().decode(base64CipherText);
            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH_BYTES);
            byte[] cipherText = Arrays.copyOfRange(combined, IV_LENGTH_BYTES, combined.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("계좌번호 복호화 실패", e);
        }
    }

    /** 앞·뒤 3자리만 남기고 마스킹한다. 길이가 6 이하면 전체를 마스킹한다(방어적 처리). */
    public String mask(String plainAccountNumber) {
        if (plainAccountNumber == null) {
            return null;
        }
        if (plainAccountNumber.length() <= 6) {
            return "*".repeat(plainAccountNumber.length());
        }
        String prefix = plainAccountNumber.substring(0, 3);
        String suffix = plainAccountNumber.substring(plainAccountNumber.length() - 3);
        return prefix + "*".repeat(plainAccountNumber.length() - 6) + suffix;
    }

    /** 저장된 암호문을 복호화해 곧바로 마스킹한다 — 원문을 호출자에게 내보내지 않는다. */
    public String decryptAndMask(String encryptedAccountNumber) {
        return encryptedAccountNumber == null ? null : mask(decrypt(encryptedAccountNumber));
    }
}
