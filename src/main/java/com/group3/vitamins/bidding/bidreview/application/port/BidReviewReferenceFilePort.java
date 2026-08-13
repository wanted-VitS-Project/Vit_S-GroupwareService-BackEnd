package com.group3.vitamins.bidding.bidreview.application.port;

import java.util.List;

public interface BidReviewReferenceFilePort {

    List<ReferenceFileSnapshot> findAccessibleFiles(
            Long companyId,
            List<Long> referenceFileIds
    );

    // Worker 작업 조회(§Python 입찰 문서 검토 작업 조회)가 쓰는 단명 다운로드 URL 발급.
    // ⚠️ 이 메서드 뒤에 실제 조회 대상(지금은 bid_reference_file)이 격리돼 있다 — 사내 문서함으로
    // 전환되면 이 포트 시그니처는 그대로 두고 구현 어댑터만 교체한다.
    List<DownloadableReferenceFile> findDownloadableFiles(
            Long companyId,
            List<Long> referenceFileIds
    );

    record ReferenceFileSnapshot(
            Long referenceFileId,
            String fileName,
            String uploadStatus,
            String indexStatus
    ) {

        public boolean isReady() {
            return "COMPLETED".equals(uploadStatus)
                    && "COMPLETED".equals(indexStatus);
        }
    }

    record DownloadableReferenceFile(
            Long referenceFileId,
            String fileName,
            String downloadUrl
    ) {
    }
}