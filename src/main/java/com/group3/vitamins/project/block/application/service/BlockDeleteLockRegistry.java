package com.group3.vitamins.project.block.application.service;

import com.group3.vitamins.project.block.application.port.BlockDeleteLockPort;
import com.group3.vitamins.project.block.domain.model.BlockType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class BlockDeleteLockRegistry {

    private final Map<BlockType, List<BlockDeleteLockPort>> ports;

    /**
     * 타입당 여러 어댑터를 허용한다 (groupingBy) — BlockDetailRegistry 와 다른 점이다.
     * FILE 블록은 파일 도메인이 아니라 결재 도메인이 잠그므로 한 타입에 둘이 붙을 수 있다.
     *
     * <p>구현체가 0개여도 스프링이 빈 리스트를 주입한다 (BlockDeleteLockRegistryWiringTest 로 확인).
     * 잠금 어댑터는 아직 하나도 없고, 그 상태가 정 상 동작이다 — 모든 타입이 잠금 없이 삭제된다.
     */
    public BlockDeleteLockRegistry(List<BlockDeleteLockPort> ports) {
        this.ports = ports.stream()
                .collect(Collectors.groupingBy(BlockDeleteLockPort::supportedType));
    }

    /** 하나라도 잠갔으면 잠긴 것이다. 어댑터가 없는 타입은 항상 false. */
    public boolean isLocked(BlockType type, Long blockId, Long typeId) {
        return ports.getOrDefault(type, List.of()).stream()
                .anyMatch(port -> port.isDeleteLocked(blockId, typeId));
    }
}