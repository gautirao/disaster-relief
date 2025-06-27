package com.disasterrelief.core.saga;

import com.disasterrelief.commandcenter.domain.event.CommandAcknowledgedEvent;
import com.disasterrelief.commandcenter.domain.event.CommandIssuedEvent;
import com.disasterrelief.commandcenter.domain.valueobject.Message;
import com.disasterrelief.commandcenter.saga.CommandSaga;
import com.disasterrelief.commandcenter.saga.SagaStatus;
import com.disasterrelief.core.event.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class SagaManagerTest {

    private SagaManager<UUID, CommandSaga> sagaManager;

    private UUID commandId;
    private UUID teamId;
    private UUID member1;
    private UUID member2;
    private Set<UUID> expectedAcknowledgers;
    private Instant deadline;

    @BeforeEach
    void setup() {
        commandId = UUID.randomUUID();
        teamId = UUID.randomUUID();
        member1 = UUID.randomUUID();
        member2 = UUID.randomUUID();

        expectedAcknowledgers = Set.of(member1, member2);
        deadline = Instant.now().plusSeconds(3600);

        Function<DomainEvent, UUID> sagaIdExtractor = event -> {
            if (event instanceof CommandIssuedEvent issued) {
                return issued.commandId();
            } else if (event instanceof CommandAcknowledgedEvent ack) {
                return ack.commandId();
            }
            return null;
        };

        Function<UUID, CommandSaga> sagaFactory = id -> new CommandSaga(id, teamId, expectedAcknowledgers, deadline);
        Predicate<DomainEvent> isStartingEvent = event -> event instanceof CommandIssuedEvent;

        sagaManager = new SagaManager<>(sagaIdExtractor, sagaFactory,isStartingEvent);
    }

    // Helper methods to create events
    private CommandIssuedEvent createCommandIssuedEvent(UUID cmdId, Instant deadline) {
        Instant now = Instant.now();
        return new CommandIssuedEvent(
                cmdId,
                teamId,
                new Message("Please acknowledge", UUID.randomUUID(), now),
                now,
                deadline,
                UUID.randomUUID(),
                expectedAcknowledgers
        );
    }

    private CommandAcknowledgedEvent createCommandAcknowledgedEvent(UUID cmdId, UUID member) {
        return new CommandAcknowledgedEvent(
                cmdId,
                teamId,
                member,
                Instant.now()
        );
    }

    @Nested
    class SuccessfulScenarios {

        @Test
        void sagaStartsAndCompletesSuccessfully() {
            // Given a CommandIssuedEvent, saga starts
            CommandIssuedEvent issuedEvent = createCommandIssuedEvent(commandId, deadline);
            sagaManager.handleEvent(issuedEvent);

            // Saga is active and pending
            Map<UUID, CommandSaga> activeSagas = sagaManager.getActiveSagas();
            assertEquals(1, activeSagas.size());
            CommandSaga saga = activeSagas.get(commandId);
            assertNotNull(saga);
            assertFalse(saga.isCompleted());

            // When first member acknowledges, saga remains pending
            sagaManager.handleEvent(createCommandAcknowledgedEvent(commandId, member1));
            assertFalse(saga.isCompleted());

            // When all expected members acknowledge, saga completes and is removed
            sagaManager.handleEvent(createCommandAcknowledgedEvent(commandId, member2));
            assertTrue(saga.isCompleted());
            assertFalse(sagaManager.getActiveSagas().containsKey(commandId));
        }

        @Test
        void sagaReplaysEvents() {
            // Given a sequence of past events (CommandIssuedEvent + acknowledgments)
            CommandIssuedEvent issuedEvent = createCommandIssuedEvent(commandId, deadline);
            CommandAcknowledgedEvent ack1 = createCommandAcknowledgedEvent(commandId, member1);
            CommandAcknowledgedEvent ack2 = createCommandAcknowledgedEvent(commandId, member2);

            List<DomainEvent> pastEvents = List.of(issuedEvent, ack1, ack2);

            // When events are replayed
            sagaManager.replayEvents(pastEvents);

            // Then saga reconstructs state and completes
            // Then saga is removed from active sagas
            assertTrue(sagaManager.getActiveSagas().isEmpty());
        }
    }

    @Nested
    class FailureScenarios {

        @Test
        void ignoresEventsForOtherCommands() {
            // Given an event with an unrelated commandId
            UUID otherCommandId = UUID.randomUUID();
            CommandAcknowledgedEvent unrelatedAck = createCommandAcknowledgedEvent(otherCommandId, member1);

            // When handled by SagaManager
            sagaManager.handleEvent(unrelatedAck);

            // Then no saga is created or updated
            // Then active saga list remains empty
            assertTrue(sagaManager.getActiveSagas().isEmpty());
        }

        @Test
        void duplicateAcknowledgementsDoNotAffectSaga() {
            // Given a member acknowledges multiple times
            CommandIssuedEvent issuedEvent = createCommandIssuedEvent(commandId, deadline);
            sagaManager.handleEvent(issuedEvent);
            CommandSaga saga = sagaManager.getActiveSagas().get(commandId);
            assertNotNull(saga);

            CommandAcknowledgedEvent ack = createCommandAcknowledgedEvent(commandId, member1);

            // When saga handles duplicate acknowledgments
            sagaManager.handleEvent(ack);
            sagaManager.handleEvent(ack);

            // Then it does not affect saga status or acknowledged list redundantly
            assertEquals(1, saga.getAcknowledgedBy().size());
            assertFalse(saga.isCompleted());
        }

        @Test
        void missingExpectedAcknowledgerKeepsSagaPending() {
            // Given one expected acknowledger never acknowledges within deadline
            CommandIssuedEvent issuedEvent = createCommandIssuedEvent(commandId, deadline);
            sagaManager.handleEvent(issuedEvent);
            CommandSaga saga = sagaManager.getActiveSagas().get(commandId);
            assertNotNull(saga);

            // When only one member acknowledges within deadline
            sagaManager.handleEvent(createCommandAcknowledgedEvent(commandId, member1));

            // Then saga remains in pending status
            assertEquals(SagaStatus.PENDING, saga.getStatus());
            assertFalse(saga.isCompleted());
        }

        @Test
        void nullOrInvalidEventsAreIgnored() {
            // Given null event
            sagaManager.handleEvent(null);

            // Given event with null saga id
            DomainEvent unknownEvent = new DomainEvent() {
                @Override
                public UUID aggregateId() {
                    return UUID.randomUUID();
                }

                @Override
                public Instant occurredAt() {
                    return Instant.now();
                }
            };
            sagaManager.handleEvent(unknownEvent);

            // Then no saga is created or crash occurs
            assertTrue(sagaManager.getActiveSagas().isEmpty());
        }

        @Test
        void partialAcknowledgementBeyondDeadlineCompensatesSaga() throws InterruptedException {
            // Given some acknowledgements arrive after deadline (simulate by past deadline)
            Instant pastDeadline = Instant.now().minusSeconds(1);
            CommandSaga saga = new CommandSaga(commandId, teamId, expectedAcknowledgers, pastDeadline);

            // When saga handles late acknowledgements
            saga.handle(createCommandAcknowledgedEvent(commandId, member1));

            // Then saga is compensated due to timeout
            assertEquals(SagaStatus.COMPENSATED, saga.getStatus());

            // Optional: additional late acks ignored or do not affect saga status
            saga.handle(createCommandAcknowledgedEvent(commandId, member2));
            assertEquals(SagaStatus.COMPENSATED, saga.getStatus());
        }

        @Test
        void emptyExpectedAcknowledgerSetThrowsException() {
            // Given CommandIssuedEvent with empty expected acknowledgers set
            Instant futureDeadline = Instant.now().plusSeconds(3600);

            // When saga is created
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    new CommandSaga(commandId, teamId, Collections.emptySet(), futureDeadline)
            );

            // Then saga initialization fails with message
            assertTrue(ex.getMessage().contains("expectedAcknowledgers must not be null or empty"));
        }

        @Test
        void replayingEventsWithMissingIntermediateEventsKeepsSagaIncomplete() {
            // Given replay of incomplete event stream missing CommandIssuedEvent
            CommandAcknowledgedEvent ack1 = createCommandAcknowledgedEvent(commandId, member1);

            // When replaying events without CommandIssuedEvent
            sagaManager.replayEvents(List.of(ack1));

            // Then saga fails to reconstruct or stays incomplete (no saga created)
            assertTrue(sagaManager.getActiveSagas().isEmpty());
        }
    }
}
