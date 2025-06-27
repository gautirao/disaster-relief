package com.disasterrelief.core.event;


import java.time.Instant;
import java.util.UUID;

public class SagaCompensatedEvent implements DomainEvent {
    private final UUID commandId;
    private final Instant compensatedAt;
    private final String reason;

    public SagaCompensatedEvent(UUID commandId, Instant compensatedAt, String reason) {
        this.commandId = commandId;
        this.compensatedAt = compensatedAt;
        this.reason = reason;
    }

    @Override
    public UUID aggregateId() {
        return commandId;
    }

    @Override
    public Instant occurredAt() {
        return compensatedAt;
    }

    public String getReason() {
        return reason;
    }
}
