package com.group3.vitamins.bidding.bidnotice.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.bidding.bidnotice.application.port.BidNoticeListCachePort;
import com.group3.vitamins.bidding.bidnotice.application.query.SearchBidNoticesQuery;
import com.group3.vitamins.bidding.bidnotice.application.result.BidNoticeListResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Slf4j
@Component
public class RedisBidNoticeListCacheAdapter implements BidNoticeListCachePort {

    private static final String VERSION_KEY_PREFIX = "bidding:notices:version:";
    private static final String DATA_KEY_PREFIX = "bidding:notices:data:";
    private static final String INITIAL_VERSION = "0";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final BidNoticeCacheProperties properties;

    public RedisBidNoticeListCacheAdapter(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            BidNoticeCacheProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public CacheLookup lookup(Long companyId, SearchBidNoticesQuery query) {
        try {
            String version = currentVersion(companyId);
            String cached = redisTemplate.opsForValue().get(
                    dataKey(companyId, version, query)
            );
            if (cached == null) {
                return new CacheLookup(version, Optional.empty());
            }
            String latestVersion = currentVersion(companyId);
            if (!version.equals(latestVersion)) {
                return new CacheLookup(latestVersion, Optional.empty());
            }
            return new CacheLookup(
                    version,
                    Optional.of(objectMapper.readValue(cached, BidNoticeListResult.class))
            );
        } catch (RuntimeException | JsonProcessingException exception) {
            log.warn("Bidding notice list cache lookup failed. companyId={}", companyId, exception);
            return CacheLookup.unavailable();
        }
    }

    @Override
    public void put(
            Long companyId,
            SearchBidNoticesQuery query,
            String version,
            BidNoticeListResult result
    ) {
        if (version == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(
                    dataKey(companyId, version, query),
                    objectMapper.writeValueAsString(result),
                    properties.ttl()
            );
        } catch (RuntimeException | JsonProcessingException exception) {
            log.warn("Bidding notice list cache write failed. companyId={}", companyId, exception);
        }
    }

    @Override
    public boolean invalidate(Long companyId) {
        try {
            redisTemplate.opsForValue().increment(versionKey(companyId));
            return true;
        } catch (RuntimeException exception) {
            log.warn("Bidding notice list cache invalidation failed. companyId={}", companyId, exception);
            return false;
        }
    }

    private String currentVersion(Long companyId) {
        String version = redisTemplate.opsForValue().get(versionKey(companyId));
        return version == null ? INITIAL_VERSION : version;
    }

    private String versionKey(Long companyId) {
        return VERSION_KEY_PREFIX + companyId;
    }

    private String dataKey(
            Long companyId,
            String version,
            SearchBidNoticesQuery query
    ) throws JsonProcessingException {
        CacheCriteria criteria = new CacheCriteria(
                query.startDate(), query.endDate(), query.noticeAgency(),
                query.businessCategoryId(), query.region(), query.deadlineSoon(),
                query.keyword(), query.noticeStatus(), query.favorite(), query.sort(),
                query.page(), query.size()
        );
        return DATA_KEY_PREFIX + companyId + ":v" + version + ":" + hash(criteria);
    }

    private String hash(CacheCriteria criteria) throws JsonProcessingException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(
                    objectMapper.writeValueAsString(criteria).getBytes(StandardCharsets.UTF_8)
            );
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private record CacheCriteria(
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            String noticeAgency,
            Long businessCategoryId,
            String region,
            Boolean deadlineSoon,
            String keyword,
            String noticeStatus,
            Boolean favorite,
            String sort,
            int page,
            int size
    ) {
    }
}
