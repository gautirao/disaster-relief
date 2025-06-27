package com.disasterrelief.commandcenter.infrastructure.store;

import com.disasterrelief.commandcenter.domain.event.DomainEvent;

import java.util.*;

public interface EventStore {
    List<DomainEvent> loadEvents(UUID aggregateId);
    void append(DomainEvent event);
}
