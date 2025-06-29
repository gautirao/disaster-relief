package com.disasterrelief.commandcenter.persistence;

import com.disasterrelief.core.eventstore.PersistedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaPersistedEventRepository extends JpaRepository<PersistedEvent, UUID> {
    List<PersistedEvent> findBySagaId(UUID sagaId);
}
