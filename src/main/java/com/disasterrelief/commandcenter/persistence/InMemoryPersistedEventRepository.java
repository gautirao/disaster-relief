package com.disasterrelief.commandcenter.persistence;

import com.disasterrelief.core.eventstore.PersistedEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryPersistedEventRepository implements PersistedEventRepository {

    // Map<SagaId, List of PersistedEvent>
    private final Map<UUID, List<PersistedEvent>> storage = new ConcurrentHashMap<>();

    @Override
    public void save(PersistedEvent event) {
        storage.computeIfAbsent(event.getSagaId(), k -> Collections.synchronizedList(new ArrayList<>()))
                .add(event);
    }

    @Override
    public List<PersistedEvent> findBySagaId(UUID sagaId) {
        return storage.getOrDefault(sagaId, Collections.emptyList());
    }
}
