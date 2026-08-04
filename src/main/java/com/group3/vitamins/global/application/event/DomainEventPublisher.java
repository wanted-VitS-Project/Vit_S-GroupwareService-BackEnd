package com.group3.vitamins.global.application.event;

import com.group3.vitamins.global.domain.event.DomainEvent;

public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
