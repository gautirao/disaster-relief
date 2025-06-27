package com.disasterrelief.commandcenter.persistence;

import com.disasterrelief.core.eventstore.PersistedEvent;

import java.util.List;
import java.util.UUID;

public interface PersistedEventRepository {
    void save(PersistedEvent event);
    List<PersistedEvent> findBySagaId(UUID sagaId);
}
