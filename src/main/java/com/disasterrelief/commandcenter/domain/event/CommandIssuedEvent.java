package com.disasterrelief.commandcenter.domain.event;

import com.disasterrelief.commandcenter.domain.valueobject.Message;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event emitted when a command is issued to a team.
 */
public record CommandIssuedEvent(
        UUID commandId,
        UUID teamId,
        Message message,
        Instant deadline,
        Instant occurredAt
) implements DomainEvent {

    @Override
    public UUID aggregateId() {
        return commandId;
    }
}
