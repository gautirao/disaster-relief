package com.disasterrelief.commandcenter.domain.command;

import com.disasterrelief.commandcenter.domain.entity.TeamMember;

import java.util.List;
import java.util.UUID;

/**
 * Command to create a new team.
 */
public record CreateTeamCommand(
        UUID teamId,
        String name,
        List<TeamMember> members
) {
    public CreateTeamCommand {
        if (teamId == null) throw new IllegalArgumentException("Team ID is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Team name is required");
        if (members == null || members.isEmpty()) throw new IllegalArgumentException("At least one team member is required");
    }
}
