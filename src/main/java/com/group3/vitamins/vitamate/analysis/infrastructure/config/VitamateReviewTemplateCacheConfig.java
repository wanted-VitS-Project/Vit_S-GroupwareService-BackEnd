package com.group3.vitamins.vitamate.analysis.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

// 비타메이트 검토 템플릿(vitamate_review_type/vitamate_review_template)은 Flyway 배포로만
// 바뀌는 전역 참조 데이터라 — 이 앱 안에 이 두 테이블을 쓰는 API가 없다 — 인메모리 캐시로 충분하다.
// ⚠️ 나중에 이 템플릿을 수정하는 관리자 API가 생기면, 그 쓰기 경로에 반드시 @CacheEvict(allEntries=true,
// cacheNames={REVIEW_TEMPLATE_GROUPS_CACHE, ACTIVE_TEMPLATES_CACHE})를 붙여야 한다 — 안 붙이면 배포 없이
// 값을 고쳐도 캐시가 갱신될 때까지(만료 또는 재기동) 이전 값이 계속 나간다.
@Configuration
@EnableCaching
public class VitamateReviewTemplateCacheConfig {

    public static final String REVIEW_TEMPLATE_GROUPS_CACHE = "vitamateReviewTemplateGroups";
    public static final String ACTIVE_TEMPLATES_CACHE = "vitamateActiveTemplates";

    @Bean
    public CacheManagerCustomizer<CaffeineCacheManager> vitamateReviewTemplateCacheCustomizer() {
        return cacheManager -> {
            cacheManager.setCacheNames(java.util.List.of(REVIEW_TEMPLATE_GROUPS_CACHE, ACTIVE_TEMPLATES_CACHE));
            // 전체를 합쳐도 수십 행 수준이라 크기 상한은 여유롭게, 배포 없이 값이 바뀌었을 가능성에
            // 대비해 만료 시간을 무한이 아니라 1시간으로 둔다.
            cacheManager.setCaffeine(Caffeine.newBuilder()
                    .maximumSize(200)
                    .expireAfterWrite(1, TimeUnit.HOURS));
        };
    }
}
