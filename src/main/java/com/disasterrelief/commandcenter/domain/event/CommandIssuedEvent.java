package com.disasterrelief.commandcenter.domain.event;

import com.disasterrelief.core.event.DomainEvent;
import com.disasterrelief.commandcenter.domain.valueobject.Message;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record CommandIssuedEvent(
        UUID commandId,
        UUID teamId,
        Message message,
        Instant issuedAt,
        Instant deadline,
        UUID issuedBy,
        Set<UUID> expectedAcknowledgerIds
) implements DomainEvent {

    public CommandIssuedEvent(
            UUID commandId,
            UUID teamId,
            Message message,
            Instant issuedAt,
            Instant deadline,
            UUID issuedBy,
            Set<UUID> expectedAcknowledgerIds
    ) {
        if (commandId == null) throw new IllegalArgumentException("commandId must not be null");
        if (teamId == null) throw new IllegalArgumentException("teamId must not be null");
        if (message == null ) throw new IllegalArgumentException("message must not be null or blank");
        if (issuedAt == null) throw new IllegalArgumentException("issuedAt must not be null");
        if (issuedBy == null) throw new IllegalArgumentException("issuedBy must not be null");
        if (deadline == null) throw new IllegalArgumentException("deadline must not be null");
        if (expectedAcknowledgerIds == null || expectedAcknowledgerIds.isEmpty()) {
            throw new IllegalArgumentException("expectedAcknowledgerIds must not be null or empty");
        }
        this.commandId = commandId;
        this.teamId = teamId;
        this.message = message;
        this.issuedAt = issuedAt;
        this.issuedBy = issuedBy;
        this.deadline = deadline;
        this.expectedAcknowledgerIds = expectedAcknowledgerIds;
    }

    @Override
    public UUID aggregateId() {
        return commandId;
    }

    @Override
    public Instant occurredAt() {
        return issuedAt;
    }
}

