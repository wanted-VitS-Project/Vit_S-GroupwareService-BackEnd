package com.group3.vitamins.project.block.application.service;

import com.group3.vitamins.project.block.application.port.BlockDeleteLockPort;
import com.group3.vitamins.project.block.domain.model.BlockType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** 잠금 어댑터 구현체가 0개인 상태에서 레지스트리가 기동하는지 확인한다 (DB 불필요). */
class BlockDeleteLockRegistryWiringTest {

    @Test
    @DisplayName("구현체가 0개여도 컨텍스트가 뜨고 모든 타입이 잠기지 않은 것으로 판정된다")
    void bootsWithNoAdapters() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(BlockDeleteLockRegistry.class)) {

            BlockDeleteLockRegistry registry = context.getBean(BlockDeleteLockRegistry.class);

            assertThat(registry.isLocked(BlockType.TEXT, 1L, 10L)).isFalse();
            assertThat(registry.isLocked(BlockType.PAYMENT_CONFIRM, 2L, null)).isFalse();
        }
    }

    @Test
    @DisplayName("어댑터가 붙으면 그 타입만 잠긴다")
    void locksOnlySupportedType() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                BlockDeleteLockRegistry.class, StubLockAdapter.class)) {

            BlockDeleteLockRegistry registry = context.getBean(BlockDeleteLockRegistry.class);

            assertThat(registry.isLocked(BlockType.PAYMENT_CONFIRM, 1L, 10L)).isTrue();
            assertThat(registry.isLocked(BlockType.TEXT, 1L, 10L)).isFalse();
        }
    }

    @Test
    @DisplayName("List 로 받는 컴포넌트 생성자는 구현체 0개일 때 어떻게 되는가 — 고치기 전 코드 모양")
    void listInjectionWithNoCandidates() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(ListInjectedRegistry.class)) {

            ListInjectedRegistry registry = context.getBean(ListInjectedRegistry.class);
            assertThat(registry.ports()).isEmpty();
        }
    }

    @Component
    static class StubLockAdapter implements BlockDeleteLockPort {

        @Override
        public BlockType supportedType() {
            return BlockType.PAYMENT_CONFIRM;
        }

        @Override
        public boolean isDeleteLocked(Long blockId, Long typeId) {
            return true;
        }
    }

    /** 고치기 전 코드와 같은 모양 — 대조군이다. */
    @Component
    static class ListInjectedRegistry {

        private final Map<BlockType, List<BlockDeleteLockPort>> ports;

        ListInjectedRegistry(List<BlockDeleteLockPort> ports) {
            this.ports = ports.stream()
                    .collect(Collectors.groupingBy(BlockDeleteLockPort::supportedType));
        }

        Map<BlockType, List<BlockDeleteLockPort>> ports() {
            return ports;
        }
    }

}
