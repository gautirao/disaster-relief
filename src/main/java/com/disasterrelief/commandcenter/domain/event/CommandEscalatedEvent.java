package com.disasterrelief.commandcenter.domain.event;

import com.disasterrelief.core.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record CommandEscalatedEvent(
        UUID commandId,
        UUID teamId,
        String reason,
        Instant occurredAt
) implements DomainEvent {

    @Override
    public UUID aggregateId() {
        return commandId;
    }
}
