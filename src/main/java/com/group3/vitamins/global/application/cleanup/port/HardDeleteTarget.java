package com.group3.vitamins.global.application.cleanup.port;

import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;

/**
 * 전역 하드 딜리트 실행기({@code HardDeleteExecutor})가 호출할 도메인별 삭제 대상.
 * 도메인은 이 인터페이스를 직접 구현하지 않고 {@link com.group3.vitamins.global.application.cleanup.DefaultHardDeleteTarget}을
 * {@code @Bean}으로 등록해 참여한다.
 */
public interface HardDeleteTarget {

    /** 로그와 식별에 사용할 삭제 대상 이름. */
    String targetName();

    /** 데이터 보존 기간. */
    TemporalAmount retention();

    /** 기준 시각 이전의 데이터를 하드 딜리트하고 삭제 건수를 반환한다. */
    int hardDeleteBefore(LocalDateTime threshold);
}
