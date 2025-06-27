package com.disasterrelief.core.eventstore;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class PersistedEvent {
    private final UUID id;            // Unique event ID
    private final UUID sagaId;        // Saga or aggregate this event belongs to
    private final String eventType;   // Fully qualified event class name
    private final String eventPayload; // Serialized event data (JSON)
    private final Instant createdAt; // Timestamp when event was persisted
}
