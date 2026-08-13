package com.group3.vitamins.bidding.referencefile.application.port;

public interface BidReferenceFileActiveUsagePort {

    boolean existsActiveReviewUsage(Long companyId, Long referenceFileId);
}