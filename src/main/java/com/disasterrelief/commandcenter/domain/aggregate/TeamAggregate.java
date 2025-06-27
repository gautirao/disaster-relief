package com.disasterrelief.commandcenter.domain.aggregate;

import com.disasterrelief.commandcenter.domain.command.CreateTeamCommand;
import com.disasterrelief.commandcenter.domain.entity.TeamMember;
import com.disasterrelief.core.event.DomainEvent;
import com.disasterrelief.commandcenter.domain.event.TeamCreatedEvent;

import java.util.*;

public class TeamAggregate {

    private UUID teamId;
    private String name;
    private List<TeamMember> members = new ArrayList<>();
    private boolean created = false;

    public static TeamAggregate rehydrate(List<DomainEvent> history) {
        TeamAggregate agg = new TeamAggregate();
        for (DomainEvent event : history) {
            agg.apply(event);
        }
        return agg;
    }

    private void apply(DomainEvent event) {
        if (event instanceof TeamCreatedEvent e) {
            this.teamId = e.teamId();
            this.name = e.name();
            this.members = new ArrayList<>(e.members());
            this.created = true;
        }
        // Add more event types as needed
    }

    public List<DomainEvent> handle(CreateTeamCommand command) {
        if (created) {
            throw new IllegalStateException("Team already created");
        }

        return List.of(new TeamCreatedEvent(
                command.teamId(),
                command.name(),
                command.members(),
                command.issuedBy(),
                new Date().toInstant()
        ));
    }

    // Getters for unit testing (optional)
    public UUID getTeamId() { return teamId; }
    public String getName() { return name; }
    public List<TeamMember> getMembers() { return members; }
}
