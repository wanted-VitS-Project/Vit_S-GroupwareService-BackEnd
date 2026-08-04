package com.group3.vitamins.account.domain;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 관리자 재설정·초기 발급용 임시 비밀번호 생성기.
 *
 * <p>사용자는 이 비밀번호로 한 번 로그인한 뒤 {@code must_change_password} 게이트에 걸려
 * 곧바로 새 비밀번호를 정한다. 그래도 전달 중 노출을 대비해 <b>추측 불가능</b>해야 하므로 {@link SecureRandom}
 * 으로 만든다.
 *
 * <p>구성은 로그인 비밀번호 정책(8자 이상 · 영문·숫자·특수문자 포함)을 항상 만족하도록
 * 각 종류를 최소 1개씩 넣는다 — 재설정 계정이 정책 미달로 다음 단계에서 막히는 일이 없게 한다.
 */
@Component
public final class TempPasswordGenerator {

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";   // 헷갈리는 I·O 제외
    private static final String LOWER = "abcdefghijkmnpqrstuvwxyz";   // 헷갈리는 l·o 제외
    private static final String DIGIT = "23456789";                   // 헷갈리는 0·1 제외
    private static final String SPECIAL = "!@#$%^&*";                 // 메일·셸에서 안전한 범위
    private static final String ALL = UPPER + LOWER + DIGIT + SPECIAL;
    private static final int LENGTH = 12;

    private final SecureRandom random = new SecureRandom();

    /** 12자리 임시 비밀번호. 대문자·소문자·숫자·특수문자를 각각 최소 1개 포함한다. */
    public String generate() {
        List<Character> chars = new ArrayList<>(LENGTH);
        chars.add(pick(UPPER));
        chars.add(pick(LOWER));
        chars.add(pick(DIGIT));
        chars.add(pick(SPECIAL));
        for (int i = chars.size(); i < LENGTH; i++) {
            chars.add(pick(ALL));
        }
        // 앞 4자리가 항상 대/소/숫자/특수 순이 되지 않도록 섞는다.
        Collections.shuffle(chars, random);

        StringBuilder sb = new StringBuilder(LENGTH);
        chars.forEach(sb::append);
        return sb.toString();
    }

    private char pick(String pool) {
        return pool.charAt(random.nextInt(pool.length()));
    }
}
