package com.group3.vitamins.global.infrastructure.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Argon2id 해시 파라미터와 동시 실행 제어 설정.
 *
 * <p>값을 코드에 박지 않고 설정으로 뺀 이유: 배포 대상 하드웨어에서 실측한 뒤 조정해야 하기 때문이다.
 * 실측 도구는 {@code ./gradlew benchmark} (Argon2BenchmarkTest).
 *
 * <p>기본값 근거 (2026-08-03 실측 · 노트북 14코어 기준 잠정치):
 * <ul>
 *   <li>{@code memoryKb=65536} — RFC 9106 이 메모리 제약 환경에 권장하는 값.
 *       <b>GPU 방어의 본체라 속도가 필요해도 이 값은 깎지 않는다.</b> 대신 iterations 를 내린다</li>
 *   <li>{@code iterations=3} — 실측 156ms. 배포 대상이 3배 느려도 0.5초 이내</li>
 *   <li>{@code parallelism=1} — BouncyCastle 은 lane 을 스레드로 돌리지 않는다.
 *       실측상 p=2 와 속도 차이가 1%(노이즈) 라 올릴 이유가 없다</li>
 *   <li>{@code permits=2} — 세마포어는 성능 손잡이가 아니라 <b>메모리 안전밸브</b>다.
 *       실측상 permit 을 늘리면 처리 시간·힙이 모두 나빠진다 (병목이 CPU 가 아니라 메모리 대역폭)</li>
 * </ul>
 *
 * <p>⚠️ 힙 사이징 공식은 {@code permits × memoryKb × 2.5} 다. 2.5 배는 GC 가 회수하기 전에
 * 쌓이는 분량으로, 실측으로 확인했다. 이론값으로 계산하면 2.5배 과소평가한다.
 */
@ConfigurationProperties(prefix = "security.argon2")
public record Argon2Properties(
        int memoryKb,
        int iterations,
        int parallelism,
        int saltLength,
        int hashLength,

        /** 동시에 해시를 돌릴 수 있는 최대 개수. 메모리 상한 = permits × memoryKb × 2.5 */
        int permits,

        /** 로그인 경로의 세마포어 대기 한도. 초과하면 503 을 준다 */
        Duration loginWait,

        /** 계정 일괄 발급처럼 요청 1건이 해시 N회를 도는 경로의 대기 한도 */
        Duration bulkWait
) {

    public Argon2Properties {
        if (memoryKb < 8192) {
            throw new IllegalArgumentException(
                    "security.argon2.memory-kb 가 너무 작다. OWASP 최소 권장은 19456(19MiB) 이다: " + memoryKb);
        }
        if (iterations < 1 || parallelism < 1 || permits < 1) {
            throw new IllegalArgumentException("iterations · parallelism · permits 는 1 이상이어야 한다");
        }
    }

    /** 이 설정으로 동시 실행이 최대일 때 예상되는 힙 점유(MB). 기동 로그에 남겨 사이징 근거를 눈에 보이게 한다. */
    public long estimatedPeakHeapMb() {
        return Math.round(permits * (memoryKb / 1024.0) * 2.5);
    }
}
