package com.disasterrelief.core.eventstore;

import com.disasterrelief.commandcenter.persistence.JpaPersistedEventRepository;
import com.disasterrelief.core.event.DomainEvent;
import com.disasterrelief.util.EventSerializationUtil;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class JpaEventStore implements EventStore {

    private final JpaPersistedEventRepository repository;
    private final Clock clock;

    public JpaEventStore(JpaPersistedEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public void append(DomainEvent event) {
        try {
            String json = EventSerializationUtil.serialize(event);
            PersistedEvent persistedEvent = PersistedEvent.builder()
                    .id(UUID.randomUUID())
                    .sagaId(event.aggregateId()) // ✅ matches your DomainEvent interface
                    .eventType(event.getClass().getName())
                    .eventPayload(json)
                    .createdAt(Instant.now(clock))
                    .build();
            repository.save(persistedEvent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist event", e);
        }
    }

    @Override
    public List<DomainEvent> readAll() {
        return repository.findAll().stream()
                .map(this::toDomainEvent)
                .collect(Collectors.toList());
    }

    @Override
    public List<DomainEvent> readByAggregateId(UUID aggregateId) {
        return repository.findBySagaId(aggregateId).stream()
                .map(this::toDomainEvent)
                .collect(Collectors.toList());
    }

    private DomainEvent toDomainEvent(com.disasterrelief.core.eventstore.PersistedEvent persistedEvent) {
        try {
            Class<?> clazz = Class.forName(persistedEvent.getEventType());
            return (DomainEvent) EventSerializationUtil.deserialize(persistedEvent.getEventPayload(), clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize event: " + persistedEvent.getEventType(), e);
        }
    }
}
