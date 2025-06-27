package com.disasterrelief.commandcenter.domain.event;


import com.disasterrelief.commandcenter.domain.entity.TeamMember;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TeamCreatedEvent(
        UUID teamId,
        String name,
        List<TeamMember> members,
        Instant occurredAt
) implements DomainEvent {

    @Override
    public UUID aggregateId() {
        return teamId;
    }
}

