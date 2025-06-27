package com.disasterrelief.commandcenter.domain.command;

import com.disasterrelief.commandcenter.domain.entity.TeamMember;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Command to create a new team. */
public record CreateTeamCommand(UUID teamId, String name, List<TeamMember> members, UUID issuedBy) {
  public CreateTeamCommand {
    if (name == null || name.isBlank()) throw new IllegalArgumentException("Name required");
    if (members == null || members.isEmpty())
      throw new IllegalArgumentException("Members required");
    if (issuedBy == null) throw new IllegalArgumentException("IssuedBy required");

    long distinctCount = members.stream().map(TeamMember::getMemberId).distinct().count();
    if (distinctCount != members.size())
      throw new IllegalArgumentException("Duplicate member IDs not allowed");
  }
}
