package com.disasterrelief.core.saga;

import com.disasterrelief.commandcenter.domain.event.CommandAcknowledgedEvent;
import com.disasterrelief.commandcenter.domain.event.CommandIssuedEvent;
import com.disasterrelief.commandcenter.domain.valueobject.Message;
import com.disasterrelief.commandcenter.saga.CommandSaga;
import com.disasterrelief.core.event.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class SagaManagerTest {

    private SagaManager<UUID, CommandSaga> sagaManager;

    private UUID commandId;
    private UUID teamId;
    private UUID member1;
    private UUID member2;

    private Set<UUID> expectedAcknowledgers;

    @BeforeEach
    void setup() {
        commandId = UUID.randomUUID();
        teamId = UUID.randomUUID();
        member1 = UUID.randomUUID();
        member2 = UUID.randomUUID();

        expectedAcknowledgers = Set.of(member1, member2);

        // Extract sagaId (commandId) from events
        Function<DomainEvent, UUID> sagaIdExtractor = (DomainEvent event) -> {
            if (event instanceof CommandIssuedEvent issued) {
                return issued.commandId();
            } else if (event instanceof CommandAcknowledgedEvent ack) {
                return ack.commandId();
            }
            return null;
        };

        // Factory to create new CommandSaga
        Function<UUID, CommandSaga> sagaFactory = (UUID id) -> new CommandSaga(commandId, teamId, expectedAcknowledgers);

        sagaManager = new SagaManager<>(sagaIdExtractor, sagaFactory);
    }

    @Nested
    class SuccessfulScenarios {

        @Test
        void sagaStartsAndCompletesSuccessfully() {
            // Given: a new command issued event starts the saga
            CommandIssuedEvent issuedEvent = new CommandIssuedEvent(
                    commandId,
                    teamId,
                    new Message("Please acknowledge", UUID.randomUUID(), Instant.now()),
                    Instant.now(),
                    Instant.now().plusSeconds(3600),
                    UUID.randomUUID(),
                    expectedAcknowledgers
            );

            // When: the saga manager handles the command issued event
            sagaManager.handleEvent(issuedEvent);

            // Then: the saga is active and not completed
            Map<UUID, CommandSaga> activeSagas = sagaManager.getActiveSagas();
            assertEquals(1, activeSagas.size());
            CommandSaga saga = activeSagas.get(commandId);
            assertNotNull(saga);
            assertFalse(saga.isCompleted());

            // Given: member 1 acknowledges the command
            CommandAcknowledgedEvent ack1 = new CommandAcknowledgedEvent(
                    commandId,
                    teamId,
                    member1,
                    Instant.now()
            );

            // When: saga manager handles first acknowledgment
            sagaManager.handleEvent(ack1);

            // Then: saga still pending
            assertFalse(saga.isCompleted());

            // Given: member 2 acknowledges the command
            CommandAcknowledgedEvent ack2 = new CommandAcknowledgedEvent(
                    commandId,
                    teamId,
                    member2,
                    Instant.now()
            );

            // When: saga manager handles second acknowledgment
            sagaManager.handleEvent(ack2);

            // Then: saga is completed and removed from active sagas
            assertTrue(saga.isCompleted());
            assertFalse(sagaManager.getActiveSagas().containsKey(commandId));
        }

        @Test
        void sagaReplaysEvents() {
            // Given: past events of command issued and all acknowledgments
            CommandIssuedEvent issuedEvent = new CommandIssuedEvent(
                    commandId,
                    teamId,
                    new Message("Please acknowledge", UUID.randomUUID(), Instant.now()),
                    Instant.now(),
                    Instant.now().plusSeconds(3600),
                    UUID.randomUUID(),
                    expectedAcknowledgers
            );

            CommandAcknowledgedEvent ack1 = new CommandAcknowledgedEvent(
                    commandId,
                    teamId,
                    member1,
                    Instant.now()
            );

            CommandAcknowledgedEvent ack2 = new CommandAcknowledgedEvent(
                    commandId,
                    teamId,
                    member2,
                    Instant.now()
            );

            List<DomainEvent> pastEvents = List.of(issuedEvent, ack1, ack2);

            // When: replaying past events
            sagaManager.replayEvents(pastEvents);

            // Then: no active sagas remain since saga completed
            assertTrue(sagaManager.getActiveSagas().isEmpty());
        }
    }

    @Nested
    class FailureScenarios {

        @Test
        void ignoresEventsForOtherCommands() {
            // Given: an acknowledgment event for a different command id
            UUID otherCommandId = UUID.randomUUID();
            CommandAcknowledgedEvent unrelatedAck = new CommandAcknowledgedEvent(
                    otherCommandId,
                    teamId,
                    member1,
                    Instant.now()
            );

            // When: saga manager handles unrelated acknowledgment event
            sagaManager.handleEvent(unrelatedAck);

            // Then: no saga created or active
            assertTrue(sagaManager.getActiveSagas().isEmpty());
        }
    }
}
