package com.group3.vitamins.global.infrastructure.config.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 설정 바인딩 검증.
 *
 * <p>{@code record} 기반 {@code @ConfigurationProperties} 는 바인딩이 실패해도 <b>컴파일은 통과</b>하고
 * 기동 시점에 터진다. DB·Redis 없이 앱을 띄울 수 없는 단계라, 여기서 미리 확인한다.
 *
 * <p>사용하는 값은 {@code application.yml} 의 기본값과 같게 맞춘다.
 */
@DisplayName("Argon2Properties 바인딩")
class Argon2PropertiesTest {

    // @EnableConfigurationProperties 가 바인딩 인프라(ConfigurationPropertiesBindingPostProcessor)까지
    // 등록하므로 별도 오토컨피그가 필요 없다.
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    @DisplayName("application.yml 기본값이 그대로 바인딩된다 (Duration 표기 포함)")
    void bindsDefaults() {
        runner.withPropertyValues(
                "security.argon2.memory-kb=65536",
                "security.argon2.iterations=3",
                "security.argon2.parallelism=1",
                "security.argon2.salt-length=16",
                "security.argon2.hash-length=32",
                "security.argon2.permits=2",
                "security.argon2.login-wait=8s",
                "security.argon2.bulk-wait=30s"
        ).run(context -> {
            assertThat(context).hasNotFailed();

            Argon2Properties properties = context.getBean(Argon2Properties.class);
            assertThat(properties.memoryKb()).isEqualTo(65536);
            assertThat(properties.iterations()).isEqualTo(3);
            assertThat(properties.parallelism()).isEqualTo(1);
            assertThat(properties.permits()).isEqualTo(2);
            assertThat(properties.loginWait()).isEqualTo(Duration.ofSeconds(8));
            assertThat(properties.bulkWait()).isEqualTo(Duration.ofSeconds(30));

            // permits(2) × 64MB × 2.5 = 320MB. 힙 사이징의 근거가 되는 값이라 함께 고정한다.
            assertThat(properties.estimatedPeakHeapMb()).isEqualTo(320);
        });
    }

    @Test
    @DisplayName("OWASP 최소 권장 미만의 memory-kb 는 기동을 막는다")
    void rejectsTooSmallMemory() {
        runner.withPropertyValues(
                "security.argon2.memory-kb=4096",
                "security.argon2.iterations=3",
                "security.argon2.parallelism=1",
                "security.argon2.salt-length=16",
                "security.argon2.hash-length=32",
                "security.argon2.permits=2",
                "security.argon2.login-wait=8s",
                "security.argon2.bulk-wait=30s"
        ).run(context -> assertThat(context)
                .hasFailed()
                .getFailure()
                .hasStackTraceContaining("OWASP"));
    }

    @Test
    @DisplayName("최소 허용값(19456) 은 통과하고 그보다 1 작으면 막힌다")
    void enforcesExactMinimumMemory() {
        runner.withPropertyValues(withMemoryKb(19456))
                .run(context -> assertThat(context).hasNotFailed());

        // 부등호 실수(<= vs <)를 잡는다. 예전엔 임계값이 8192 라 이 값이 조용히 통과했다
        runner.withPropertyValues(withMemoryKb(19455))
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("salt-length 0 은 기동을 막는다 — 모든 해시가 같은 솔트를 쓰게 된다")
    void rejectsZeroSaltLength() {
        runner.withPropertyValues(
                "security.argon2.memory-kb=65536",
                "security.argon2.iterations=3",
                "security.argon2.parallelism=1",
                "security.argon2.salt-length=0",
                "security.argon2.hash-length=32",
                "security.argon2.permits=2",
                "security.argon2.login-wait=8s",
                "security.argon2.bulk-wait=30s"
        ).run(context -> assertThat(context).hasFailed());
    }

    private String[] withMemoryKb(int memoryKb) {
        return new String[]{
                "security.argon2.memory-kb=" + memoryKb,
                "security.argon2.iterations=3",
                "security.argon2.parallelism=1",
                "security.argon2.salt-length=16",
                "security.argon2.hash-length=32",
                "security.argon2.permits=2",
                "security.argon2.login-wait=8s",
                "security.argon2.bulk-wait=30s"
        };
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(Argon2Properties.class)
    static class TestConfig {
    }
}
