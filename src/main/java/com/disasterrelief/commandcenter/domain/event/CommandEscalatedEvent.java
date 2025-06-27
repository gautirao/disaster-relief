package com.disasterrelief.commandcenter.domain.event;

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
