package com.group3.vitamins.bidding.bidreview.application.port;

import java.util.List;

public interface BidReviewReferenceFilePort {

    List<ReferenceFileSnapshot> findAccessibleFiles(
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
}