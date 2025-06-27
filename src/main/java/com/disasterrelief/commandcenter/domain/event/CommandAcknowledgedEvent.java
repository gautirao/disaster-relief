package com.disasterrelief.commandcenter.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when a team member acknowledges a command.
 */
public record CommandAcknowledgedEvent(
        UUID commandId,
        UUID teamId,
        UUID memberId,
        Instant occurredAt
) implements DomainEvent {

    @Override
    public UUID aggregateId() {
        return commandId;
    }
}
