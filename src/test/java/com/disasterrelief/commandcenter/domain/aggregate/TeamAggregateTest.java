package com.disasterrelief.commandcenter.domain.aggregate;

import com.disasterrelief.commandcenter.domain.command.CreateTeamCommand;
import com.disasterrelief.commandcenter.domain.entity.TeamMember;
import com.disasterrelief.core.event.DomainEvent;
import com.disasterrelief.commandcenter.domain.event.TeamCreatedEvent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TeamAggregateTest {

  @Nested
  class SuccessfulScenarios {
    @Test
    void shouldEmitTeamCreatedEvent() {
      UUID teamId = UUID.randomUUID();
      List<TeamMember> members =
          List.of(
              new TeamMember(UUID.randomUUID(), "Alice", "1234567890"),
              new TeamMember(UUID.randomUUID(), "Bob", "0987654321"));

      CreateTeamCommand command =
          new CreateTeamCommand(teamId, "RescueTeamA", members, UUID.randomUUID());

      TeamAggregate aggregate = new TeamAggregate();
      List<DomainEvent> events = aggregate.handle(command);

      assertEquals(1, events.size());
      assertTrue(events.get(0) instanceof TeamCreatedEvent);

      TeamCreatedEvent event = (TeamCreatedEvent) events.get(0);
      assertEquals(teamId, event.teamId());
      assertEquals("RescueTeamA", event.name());
      assertEquals(2, event.members().size());
    }

    @Test
    void shouldRehydrateAggregateFromPastEvent() {
      UUID teamId = UUID.randomUUID();
      List<TeamMember> members = List.of(new TeamMember(UUID.randomUUID(), "Alice", "1234567890"));

      TeamCreatedEvent event =
          new TeamCreatedEvent(
              teamId, "Medics", members, UUID.randomUUID(), new java.util.Date().toInstant());

      TeamAggregate rehydrated = TeamAggregate.rehydrate(List.of(event));

      assertEquals(teamId, rehydrated.getTeamId());
      assertEquals("Medics", rehydrated.getName());
      assertEquals(1, rehydrated.getMembers().size());
    }
  }

  @Nested
  class FailureScenarios {
    @Test
    void shouldFailIfTeamIsAlreadyCreated() {
      UUID teamId = UUID.randomUUID();
      List<TeamMember> members = List.of(new TeamMember(UUID.randomUUID(), "Alice", "1234567890"));

      TeamCreatedEvent event =
          new TeamCreatedEvent(
              teamId, "Ops", members, UUID.randomUUID(), new java.util.Date().toInstant());

      TeamAggregate aggregate = TeamAggregate.rehydrate(List.of(event));

      CreateTeamCommand duplicateCommand =
          new CreateTeamCommand(teamId, "Ops", members, UUID.randomUUID());

      assertThrows(IllegalStateException.class, () -> aggregate.handle(duplicateCommand));
    }
  }

  @Nested
  class EdgeCaseScenarios {
    @Test
    void shouldFailWithInvalidCommandArguments() {
      UUID teamId = UUID.randomUUID();
      UUID issuedBy = UUID.randomUUID();

      // Null or empty name
      assertThrows(
          IllegalArgumentException.class,
          () ->
              new CreateTeamCommand(
                  teamId,
                  null,
                  List.of(new TeamMember(UUID.randomUUID(), "Alice", "1234567890")),
                  issuedBy));

      assertThrows(
          IllegalArgumentException.class,
          () ->
              new CreateTeamCommand(
                  teamId,
                  "",
                  List.of(new TeamMember(UUID.randomUUID(), "Alice", "1234567890")),
                  issuedBy));

      // Null or empty members list
      assertThrows(
          IllegalArgumentException.class,
          () -> new CreateTeamCommand(teamId, "TeamX", null, issuedBy));

      assertThrows(
          IllegalArgumentException.class,
          () -> new CreateTeamCommand(teamId, "TeamX", List.of(), issuedBy));

      // Null issuedBy
      assertThrows(
          IllegalArgumentException.class,
          () ->
              new CreateTeamCommand(
                  teamId,
                  "TeamX",
                  List.of(new TeamMember(UUID.randomUUID(), "Alice", "1234567890")),
                  null));
    }

    @Test
    void shouldFailWithDuplicateMemberIds() {
      UUID teamId = UUID.randomUUID();
      UUID issuedBy = UUID.randomUUID();

      UUID memberId = UUID.randomUUID();

      List<TeamMember> duplicateMembers =
          List.of(
              new TeamMember(memberId, "Alice", "1234567890"),
              new TeamMember(memberId, "Bob", "0987654321"));

      assertThrows(
          IllegalArgumentException.class,
          () -> new CreateTeamCommand(teamId, "TeamX", duplicateMembers, issuedBy));
    }
  }
}
