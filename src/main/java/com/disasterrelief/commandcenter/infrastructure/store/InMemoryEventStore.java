package com.disasterrelief.commandcenter.infrastructure.store;

import com.disasterrelief.commandcenter.domain.event.DomainEvent;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryEventStore implements EventStore {

  private final Map<UUID, List<DomainEvent>> store = new ConcurrentHashMap<>();

    @Override
    public List<DomainEvent> loadEvents(UUID aggregateId) {
        return store.getOrDefault(aggregateId, Collections.emptyList());
    }

    @Override
    public void append(DomainEvent event) {
        store.computeIfAbsent(event.aggregateId(), k -> new ArrayList<>()).add(event);
    }
}
