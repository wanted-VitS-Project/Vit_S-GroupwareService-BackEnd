package com.group3.vitamins.project.block.application.service;

import com.group3.vitamins.project.block.application.port.BlockDetailPort;
import com.group3.vitamins.project.block.domain.model.BlockType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class BlockDetailRegistry {

    private final Map<BlockType, BlockDetailPort> ports;

    public BlockDetailRegistry(List<BlockDetailPort> ports) {
        this.ports = ports.stream().collect(Collectors.toUnmodifiableMap(
                BlockDetailPort::supportedType, Function.identity()));
    }

    /** 담당 어댑터를 찾는다. 없으면 empty — 그 타입은 상세를 만들지도 내리지도 않는다. */
    public Optional<BlockDetailPort> find(BlockType type) {
        return Optional.ofNullable(ports.get(type));
    }
}