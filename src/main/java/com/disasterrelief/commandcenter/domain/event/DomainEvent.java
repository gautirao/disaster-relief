package com.disasterrelief.commandcenter.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Marker interface for domain events.
 * All domain events must be serializable and contain minimum metadata.
 */
public interface DomainEvent {
    /**
     * ID of the aggregate this event belongs to
     */
    UUID aggregateId();

    /**
     * Timestamp when the event occurred
     */
    Instant occurredAt();

    /**
     * Type identifier (used for deserialization and routing)
     */
    default String type() {
        return this.getClass().getSimpleName();
    }
}

