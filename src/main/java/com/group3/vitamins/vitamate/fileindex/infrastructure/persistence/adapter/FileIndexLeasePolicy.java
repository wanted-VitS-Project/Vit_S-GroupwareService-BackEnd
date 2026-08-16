package com.group3.vitamins.vitamate.fileindex.infrastructure.persistence.adapter;

import java.time.Duration;

// file_index PENDING/PROCESSING 점유(lease) 정책값. 이 값을 쓰는 모든 진입점(최초 dispatch,
// PROCESSING 확인, 재시도, 청크 재저장으로 인한 재인덱싱)이 같은 값을 써야 재시도 스케줄러의
// lease 판단이 일관된다 — 한쪽만 다른 값을 쓰면 그 경로에서 등록한 PENDING 행이 실제보다
// 너무 빨리(혹은 영원히 안) 재claim 후보가 된다.
final class FileIndexLeasePolicy {

    // Gemini 429(크레딧 소진) 등 retryable 실패를 몇 번까지 자동 재시도할지 — bidding.bidsummary와
    // 동일한 상한을 따른다. DB의 chk_file_index_retry_count CHECK(0~2)와 반드시 맞춰야 한다.
    static final int MAX_RETRY_COUNT = 2;

    // dispatch·PROCESSING 확인 시점에 설정하는 점유 유효 시간. 대용량 문서의 청크 임베딩이
    // Gemini 429 백오프로 오래 걸려도 이 시간 안이면 재시도 스케줄러가 건드리지 않는다.
    static final Duration LEASE_DURATION = Duration.ofMinutes(15);

    private FileIndexLeasePolicy() {
    }
}
