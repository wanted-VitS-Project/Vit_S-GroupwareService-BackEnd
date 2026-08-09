package com.group3.vitamins.global.application.cleanup.port;

import java.time.LocalDateTime;

/** 기준 시각 이전의 데이터를 실제로 하드 딜리트하는 동작. 도메인은 리포지토리 메서드 참조나 UseCase 메서드 참조로 구현을 제공한다. */
@FunctionalInterface
public interface HardDeleteOperation {

    /** 전달받은 기준 시각 이전의 데이터를 삭제하고 삭제 건수를 반환한다. */
    int deleteBefore(LocalDateTime threshold);
}
