package com.disasterrelief.commandcenter.domain.command;

import com.disasterrelief.commandcenter.domain.entity.TeamMember;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record CreateTeamCommand(
        UUID teamId,
        String name,
        List<TeamMember> members,
        UUID issuedBy
) {
  public CreateTeamCommand {
    if (teamId == null) {
      throw new IllegalArgumentException("teamId must not be null");
    }
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Team name must not be null or blank");
    }
    if (members == null) {
      throw new IllegalArgumentException("members must not be null");
    }
    if (members.isEmpty()) {
      throw new IllegalArgumentException("Team members must not be empty");
    }
    if (issuedBy == null) {
      throw new IllegalArgumentException("issuedBy must not be null");
    }

    Set<UUID> uniqueIds = members.stream()
            .map(TeamMember::getMemberId)
            .collect(Collectors.toSet());
    if (uniqueIds.size() != members.size()) {
      throw new IllegalArgumentException("Duplicate team member IDs are not allowed");
    }
  }
}
