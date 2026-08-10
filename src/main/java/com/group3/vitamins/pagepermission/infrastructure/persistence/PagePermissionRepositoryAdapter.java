package com.group3.vitamins.pagepermission.infrastructure.persistence;

import com.group3.vitamins.pagepermission.application.port.PagePermissionRepository;
import com.group3.vitamins.pagepermission.domain.model.PageAccessLevel;
import com.group3.vitamins.pagepermission.domain.model.PageCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link PagePermissionRepository} 의 JPA 어댑터. DB {@code page_code} 는 부여 대상(BIDDING·FINANCE)만 들어가므로
 * {@link PageCode#valueOf} 매핑이 안전하다.
 */
@Repository
@RequiredArgsConstructor
public class PagePermissionRepositoryAdapter implements PagePermissionRepository {

    private final SpringDataPagePermissionRepository springDataRepository;

    @Override
    public Map<PageCode, PageAccessLevel> findGrantedLevels(String userId) {
        Map<PageCode, PageAccessLevel> levels = new EnumMap<>(PageCode.class);
        for (PagePermissionJpaEntity e : springDataRepository.findByUserId(userId)) {
            levels.put(PageCode.valueOf(e.getPageCode()), PageAccessLevel.valueOf(e.getPermission()));
        }
        return levels;
    }

    @Override
    public Map<String, PageAccessLevel> findLevels(PageCode pageCode, Collection<String> userIds) {
        Map<String, PageAccessLevel> levels = new HashMap<>();
        for (PagePermissionJpaEntity e : springDataRepository.findByPageCodeAndUserIdIn(pageCode.name(), userIds)) {
            levels.put(e.getUserId(), PageAccessLevel.valueOf(e.getPermission()));
        }
        return levels;
    }

    @Override
    public void grant(PageCode pageCode, String userId, PageAccessLevel level) {
        springDataRepository.save(new PagePermissionJpaEntity(pageCode.name(), userId, level.name()));
    }

    @Override
    public void updateLevel(PageCode pageCode, String userId, PageAccessLevel level) {
        // 위에서 존재를 확인하고 부르므로 반드시 있다. 방어적으로 없으면 조용히 넘긴다(회수 레이스).
        springDataRepository.findByPageCodeAndUserId(pageCode.name(), userId)
                .ifPresent(e -> e.changePermission(level.name()));
    }

    @Override
    public int revoke(PageCode pageCode, String userId) {
        return (int) springDataRepository.deleteByPageCodeAndUserId(pageCode.name(), userId);
    }
}
