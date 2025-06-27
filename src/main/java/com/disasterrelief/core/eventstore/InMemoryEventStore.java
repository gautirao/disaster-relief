package com.disasterrelief.core.eventstore;

import com.disasterrelief.core.event.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class InMemoryEventStore implements EventStore {

    private final List<PersistedEvent> events = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void append(DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            PersistedEvent persisted = PersistedEvent.builder()
                    .id(UUID.randomUUID())
                    .sagaId(event.aggregateId()) // saga or aggregate id
                    .eventType(event.getClass().getName())
                    .eventPayload(payload)
                    .createdAt(Instant.now())
                    .build();

            events.add(persisted);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }

    @Override
    public List<DomainEvent> readAll() {
        return deserialize(events);
    }

    @Override
    public List<DomainEvent> readByAggregateId(UUID aggregateId) {
        return deserialize(
                events.stream()
                        .filter(e -> e.getSagaId().equals(aggregateId))
                        .collect(Collectors.toList())
        );
    }

    private List<DomainEvent> deserialize(List<PersistedEvent> stored) {
        List<DomainEvent> result = new ArrayList<>();
        for (PersistedEvent e : stored) {
            try {
                Class<?> clazz = Class.forName(e.getEventType());
                DomainEvent domainEvent = (DomainEvent) objectMapper.readValue(e.getEventPayload(), clazz);
                result.add(domainEvent);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to deserialize event: " + e.getEventType(), ex);
            }
        }
        return result;
    }
}
