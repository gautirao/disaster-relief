package com.disasterrelief.commandcenter.domain.aggregates;

import com.disasterrelief.commandcenter.domain.command.CreateTeamCommand;
import com.disasterrelief.commandcenter.domain.entity.TeamMember;
import com.disasterrelief.commandcenter.domain.event.DomainEvent;
import com.disasterrelief.commandcenter.domain.event.TeamCreatedEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class TeamAggregate {
    private UUID teamId;
    private String name;
    private List<TeamMember> members;

    public List<DomainEvent> handle(CreateTeamCommand cmd) {
        if (cmd.name().isBlank()) throw new IllegalArgumentException("Team must have a name");
        return List.of(new TeamCreatedEvent(cmd.teamId(), cmd.name(), cmd.members(), Instant.now()));
    }

    public void apply(TeamCreatedEvent event) {
        this.teamId = event.teamId();
        this.name = event.name();
        this.members = event.members();
    }
}

