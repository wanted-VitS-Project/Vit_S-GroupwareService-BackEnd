package com.group3.vitamins.tools;

import com.group3.vitamins.auth.domain.PasswordPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 개발용 계정의 Argon2id 해시를 만드는 도구. <b>로컬 전용</b>이다.
 *
 * <pre>
 *   ./gradlew hashPassword
 * </pre>
 *
 * <h3>왜 해시를 레포에 커밋하지 않고 이 도구를 두나</h3>
 * 이 레포는 <b>PUBLIC</b> 이다. 개발용이라도 계정 비밀번호의 해시를 커밋하면 누구나 가져가
 * 오프라인에서 크래킹할 수 있고, 팀원이 그 비밀번호를 다른 곳에 재사용했다면 피해가 밖으로 번진다.
 * 그래서 <b>해시는 각자 자기 것을 만들어 쓴다.</b>
 *
 * <h3>⚠️ 테스트 소스셋에 있는 이유</h3>
 * {@code src/main} 에 두면 운영 jar 에 포함된다. 해시 생성기는 배포물에 들어갈 이유가 없다.
 *
 * <p>실행 시간이 0.2초쯤 걸리는 것이 정상이다 — Argon2id 는 원래 느리게 설계됐다.
 */
public final class PasswordHashGenerator {

    // ⚠️ application.yml 의 security.argon2 와 같은 값이어야 한다.
    //    다르면 여기서 만든 해시로 로그인이 안 된다. yml 을 바꾸면 여기도 바꿀 것.
    private static final int MEMORY_KB = 65536;
    private static final int ITERATIONS = 3;
    private static final int PARALLELISM = 1;
    private static final int SALT_LENGTH = 16;
    private static final int HASH_LENGTH = 32;

    private PasswordHashGenerator() {
    }

    public static void main(String[] args) throws IOException {
        String rawPassword = args.length > 0 ? args[0] : readFromStdin();

        if (rawPassword == null || rawPassword.isBlank()) {
            System.err.println("비밀번호가 비어 있다. 실행을 중단한다.");
            System.exit(1);
        }

        // 로그인·비밀번호 변경 API 가 쓰는 것과 같은 정책이다.
        // 여기서 걸러야 "해시는 만들었는데 API 가 거부하는" 상황을 피한다.
        try {
            PasswordPolicy.validate(rawPassword);
        } catch (RuntimeException e) {
            System.err.println("""
                    비밀번호가 정책을 만족하지 않는다 (.ai/api/auth.md §4).
                      · 8자 이상
                      · 영문 포함
                      · 숫자 포함
                      · 특수문자 포함""");
            System.exit(1);
        }

        // 생성자 인자 순서 주의: (saltLength, hashLength, parallelism, memory, iterations)
        Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(
                SALT_LENGTH, HASH_LENGTH, PARALLELISM, MEMORY_KB, ITERATIONS);

        // ⚠️ 원문은 절대 출력하지 않는다. 터미널 스크롤백·화면 공유로 샌다.
        System.out.println();
        System.out.println("Argon2id 파라미터  m=" + MEMORY_KB + "KB  t=" + ITERATIONS + "  p=" + PARALLELISM);
        System.out.println("해시 (account.password 컬럼에 그대로 넣는다)");
        System.out.println();
        System.out.println("  " + encoder.encode(rawPassword));
        System.out.println();
        System.out.println("같은 비밀번호라도 실행할 때마다 값이 다르다 (솔트가 매번 새로 생성된다). 정상이다.");
        System.out.println();
    }

    /**
     * Gradle 데몬 환경에서는 {@code System.console()} 이 null 이라 쓸 수 없다.
     * 표준입력으로 받되, 에코를 막지 못하므로 화면 공유 중에는 실행하지 말 것.
     */
    private static String readFromStdin() throws IOException {
        System.out.print("해시로 만들 비밀번호를 입력하세요: ");
        System.out.flush();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            return reader.readLine();
        }
    }
}
