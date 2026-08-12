package com.group3.vitamins.bidding.bidsummary.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bidding.worker")
public record BiddingWorkerAuthProperties(
        String token
) {
}