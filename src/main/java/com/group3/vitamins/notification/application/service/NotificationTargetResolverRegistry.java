package com.group3.vitamins.notification.application.service;

import com.group3.vitamins.notification.application.port.NotificationTargetResolverPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** `project.block.application.service.BlockDetailRegistry` 와 동일한 SPI 레지스트리 패턴. */
@Component
public class NotificationTargetResolverRegistry {

    private final Map<String, NotificationTargetResolverPort> resolvers;

    public NotificationTargetResolverRegistry(List<NotificationTargetResolverPort> resolvers) {
        this.resolvers = resolvers.stream().collect(Collectors.toUnmodifiableMap(
                NotificationTargetResolverPort::supportedType, Function.identity()));
    }

    /** 담당 어댑터를 찾는다. 없으면 empty — 그 타입은 아직 이동 대상 조회를 지원하지 않는다(`type=NONE`). */
    public Optional<NotificationTargetResolverPort> find(String type) {
        return Optional.ofNullable(resolvers.get(type));
    }
}
