package com.group3.vitamins.global.infrastructure.config.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Argon2 파라미터 · 세마포어 permit 실측.
 *
 * <p>이 클래스는 회귀 테스트가 아니라 <b>측정 도구</b>다. 결과가 하드웨어마다 달라 단정할 수 없으므로
 * 임계값 assert 를 걸지 않는다 (해시가 실제로 검증되는지만 확인한다).
 *
 * <p>실행:
 * <pre>
 *   ./gradlew benchmark
 * </pre>
 *
 * <p><b>노트북 결과는 참고용이다.</b> 확정값은 배포 대상(t3.medium · 2 vCPU · 4GB)에서 잰 값으로 정한다.
 *
 * <p>측정 목적 3가지:
 * <ol>
 *   <li><b>t 확정</b> — 해시 1회가 0.5초 이하면 t=3 유지, 0.5~0.8초면 t=2, 0.8초 초과면 t=1</li>
 *   <li><b>p 판정</b> — BouncyCastle 이 parallelism 을 실제 스레드로 돌리는지.
 *       p=1 과 p=2 의 지연이 같으면 스레드를 안 쓰는 것이고, 그러면 p 는 permit 산정에 영향이 없다</li>
 *   <li><b>permit 확정</b> — 처리량 상한은 vCPU 가 정하므로 permit 을 늘려도 안 빨라진다는 가정의 검증.
 *       늘어나는 것은 힙 점유와 개별 지연뿐이어야 한다</li>
 * </ol>
 */
@Tag("benchmark")
@DisplayName("Argon2 파라미터 실측")
class Argon2BenchmarkTest {

    /** GPU 방어의 본체. 속도가 필요하면 t 를 깎지 이 값은 건드리지 않는다. */
    private static final int MEMORY_KB = 65_536;   // 64MB
    private static final int SALT_LENGTH = 16;
    private static final int HASH_LENGTH = 32;

    private static final String PASSWORD = "Vit-S!benchmark-2026";

    private static final int WARMUP_ROUNDS = 3;
    private static final int SAMPLE_ROUNDS = 10;

    /** 납품 대상 회사 인원. 전원이 같은 순간 로그인하는 최악 상황을 재현한다. */
    private static final int CONCURRENT_USERS = 30;

    private static final int CORES = Runtime.getRuntime().availableProcessors();

    @Test
    @DisplayName("[1] 파라미터 조합별 단독 해시 지연")
    void measureSingleHashLatency() {
        System.out.printf("%n===== [1] 단독 해시 지연 (m=%dKB 고정 · %d회 평균 · 워밍업 %d회 제외) =====%n",
                MEMORY_KB, SAMPLE_ROUNDS, WARMUP_ROUNDS);
        System.out.printf("측정 환경: 코어 %d개 · 최대 힙 %dMB%n%n", CORES, Runtime.getRuntime().maxMemory() >> 20);
        System.out.println("  t   p |     평균 |     최소 |     최대 |  30명 동시 추정 |  판정");
        System.out.println("--------+----------+----------+----------+-----------------+-------");

        for (int iterations = 3; iterations >= 1; iterations--) {
            for (int parallelism = 1; parallelism <= 2; parallelism++) {
                List<Long> samples = measure(encoder(parallelism, iterations));

                double avgMs = average(samples) / 1_000_000.0;
                double minMs = min(samples) / 1_000_000.0;
                double maxMs = max(samples) / 1_000_000.0;

                // 처리량 상한 = 코어 수 ÷ 해시 1회 시간. 30명이 동시에 몰렸을 때 마지막 사람의 대기.
                double worstCaseSec = CONCURRENT_USERS * (avgMs / 1000.0) / CORES;

                System.out.printf("  %d   %d | %7.1fms | %7.1fms | %7.1fms | %13.1fs | %s%n",
                        iterations, parallelism, avgMs, minMs, maxMs, worstCaseSec, verdict(avgMs));
            }
        }

        System.out.printf("%n판정 기준: 0.5초 이하 → t=3 유지 / 0.5~0.8초 → t=2 / 0.8초 초과 → t=1%n");
        System.out.printf("p 판정: p=1 과 p=2 의 평균이 비슷하면 BouncyCastle 이 스레드를 쓰지 않는 것이다.%n");
    }

    @Test
    @DisplayName("[2] permit 수에 따른 처리량 · 지연 · 힙 점유")
    void measureThroughputByPermits() throws Exception {
        // 현재 확정 후보값으로 고정한다. [1] 결과에 따라 t 가 바뀌면 여기도 맞춘다.
        PasswordEncoder encoder = encoder(2, 3);
        warmUp(encoder);

        System.out.printf("%n===== [2] 동시 로그인 %d건 · permit 별 비교 (m=%dKB · t=3 · p=2) =====%n",
                CONCURRENT_USERS, MEMORY_KB);
        System.out.println("가정: 전원이 같은 순간 로그인 버튼을 누른다 (현실에서는 거의 없는 최악 상황)");
        System.out.println();
        System.out.println(" permit |  전원 처리 |   p50 체감 |   p95 체감 |  최악 체감 |  힙 증가분 |  이론 상한");
        System.out.println("--------+------------+------------+------------+------------+------------+-----------");

        for (int permits : new int[]{2, 4, 6}) {
            ConcurrencyResult result = runConcurrent(encoder, permits);

            System.out.printf(" %6d | %9.2fs | %9.2fs | %9.2fs | %9.2fs | %8dMB | %8dMB%n",
                    permits,
                    result.wallClockNanos() / 1_000_000_000.0,
                    result.percentileSeconds(50),
                    result.percentileSeconds(95),
                    result.percentileSeconds(100),
                    result.heapGrowthMb(),
                    (long) permits * (MEMORY_KB >> 10));
        }

        System.out.printf("%n· '전원 처리' 가 permit 과 무관하게 비슷하면 → 병목은 메모리가 아니라 CPU(%d코어) 다.%n", CORES);
        System.out.println("· '힙 증가분' 은 GC 타이밍에 좌우되므로 '이론 상한'(permit × 64MB) 과 정확히 같지 않다.");
        System.out.println("  이론 상한을 크게 넘지 않는지만 본다.");
    }

    // ===== 측정 =====

    private List<Long> measure(PasswordEncoder encoder) {
        warmUp(encoder);

        // 해시가 실제로 검증되는지 한 번만 확인한다 (matches 는 encode 와 비용이 같아 루프에 넣으면 시간이 2배가 된다).
        String sample = encoder.encode(PASSWORD);
        assertThat(encoder.matches(PASSWORD, sample)).isTrue();

        List<Long> samples = new ArrayList<>(SAMPLE_ROUNDS);
        for (int i = 0; i < SAMPLE_ROUNDS; i++) {
            long start = System.nanoTime();
            encoder.encode(PASSWORD);
            samples.add(System.nanoTime() - start);
        }
        return samples;
    }

    private ConcurrencyResult runConcurrent(PasswordEncoder encoder, int permits) throws Exception {
        Semaphore semaphore = new Semaphore(permits, true);
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CountDownLatch ready = new CountDownLatch(CONCURRENT_USERS);
        CountDownLatch fire = new CountDownLatch(1);

        HeapSampler sampler = HeapSampler.start();
        List<Future<Long>> futures = new ArrayList<>(CONCURRENT_USERS);

        for (int i = 0; i < CONCURRENT_USERS; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                fire.await();

                // 사용자 체감 = 세마포어 대기 + 해시 시간
                long start = System.nanoTime();
                semaphore.acquire();
                try {
                    encoder.encode(PASSWORD);
                } finally {
                    semaphore.release();
                }
                return System.nanoTime() - start;
            }));
        }

        ready.await();
        long begin = System.nanoTime();
        fire.countDown();

        List<Long> latencies = new ArrayList<>(CONCURRENT_USERS);
        for (Future<Long> future : futures) {
            latencies.add(future.get());
        }
        long wallClock = System.nanoTime() - begin;

        long heapGrowth = sampler.stop();
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        return new ConcurrencyResult(wallClock, latencies, heapGrowth);
    }

    private void warmUp(PasswordEncoder encoder) {
        for (int i = 0; i < WARMUP_ROUNDS; i++) {
            encoder.encode(PASSWORD);
        }
    }

    // ===== 도구 =====

    /** 인자 순서 주의: (saltLength, hashLength, parallelism, memory, iterations) */
    private PasswordEncoder encoder(int parallelism, int iterations) {
        return new Argon2PasswordEncoder(SALT_LENGTH, HASH_LENGTH, parallelism, MEMORY_KB, iterations);
    }

    private static String verdict(double avgMs) {
        if (avgMs <= 500) {
            return "t=3 가능";
        }
        if (avgMs <= 800) {
            return "t=2 권장";
        }
        return "t=1 권장";
    }

    private static double average(List<Long> values) {
        return values.stream().mapToLong(Long::longValue).average().orElse(0);
    }

    private static long min(List<Long> values) {
        return values.stream().mapToLong(Long::longValue).min().orElse(0);
    }

    private static long max(List<Long> values) {
        return values.stream().mapToLong(Long::longValue).max().orElse(0);
    }

    private record ConcurrencyResult(long wallClockNanos, List<Long> latencyNanos, long heapGrowthBytes) {

        double percentileSeconds(int percentile) {
            List<Long> sorted = latencyNanos.stream().sorted().toList();
            int index = Math.min(sorted.size() - 1, (int) Math.ceil(sorted.size() * percentile / 100.0) - 1);
            return sorted.get(Math.max(index, 0)) / 1_000_000_000.0;
        }

        long heapGrowthMb() {
            return heapGrowthBytes >> 20;
        }
    }

    /**
     * 측정 구간의 힙 사용량 피크를 20ms 간격으로 표본 추출한다.
     * GC 시점에 좌우되므로 정확한 값이 아니라 <b>자릿수 확인용</b>이다.
     */
    private static final class HeapSampler {

        private final Thread thread;
        private final AtomicLong peak = new AtomicLong();
        private final long baseline;
        private volatile boolean running = true;

        private HeapSampler() {
            System.gc();   // 기준선을 잡기 위한 정리. 벤치마크 전용이라 허용한다.
            this.baseline = usedHeap();
            this.peak.set(baseline);
            this.thread = new Thread(() -> {
                while (running) {
                    peak.accumulateAndGet(usedHeap(), Math::max);
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }, "heap-sampler");
            this.thread.setDaemon(true);
            this.thread.start();
        }

        static HeapSampler start() {
            return new HeapSampler();
        }

        /** @return 기준선 대비 힙 증가분 (bytes) */
        long stop() {
            running = false;
            thread.interrupt();
            return Math.max(0, peak.get() - baseline);
        }

        private static long usedHeap() {
            return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        }
    }
}
