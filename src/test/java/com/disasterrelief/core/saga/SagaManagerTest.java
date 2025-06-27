package com.disasterrelief.core.saga;

import com.disasterrelief.commandcenter.domain.event.CommandAcknowledgedEvent;
import com.disasterrelief.commandcenter.domain.event.CommandIssuedEvent;
import com.disasterrelief.commandcenter.domain.valueobject.Message;
import com.disasterrelief.commandcenter.saga.CommandSaga;
import com.disasterrelief.commandcenter.saga.CommandSagaTestBuilder;
import com.disasterrelief.commandcenter.saga.SagaStatus;
import com.disasterrelief.core.event.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
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
    private Clock clock;

    @BeforeEach
    void setup() {
        commandId = UUID.randomUUID();
        teamId = UUID.randomUUID();
        member1 = UUID.randomUUID();
        member2 = UUID.randomUUID();

        expectedAcknowledgers = Set.of(member1, member2);
        deadline = Instant.now().plusSeconds(3600);
        clock = Clock.systemUTC();

        Function<DomainEvent, UUID> sagaIdExtractor = event -> {
            if (event instanceof CommandIssuedEvent issued) {
                return issued.commandId();
            } else if (event instanceof CommandAcknowledgedEvent ack) {
                return ack.commandId();
            }
            return null;
        };

        Function<UUID, CommandSaga> sagaFactory = id -> CommandSagaTestBuilder.builder()
                .commandId(id)
                .teamId(teamId)
                .expectedAcknowledgers(expectedAcknowledgers)
                .deadline(deadline)
                .clock(clock)
                .build();

        Predicate<DomainEvent> isStartingEvent = event -> event instanceof CommandIssuedEvent;

        sagaManager = new SagaManager<>(sagaIdExtractor, sagaFactory, isStartingEvent);
    }

    private CommandIssuedEvent createCommandIssuedEvent(UUID cmdId, Instant deadline) {
        // Given: a valid issued command event
        Instant now = Instant.now(clock);
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
        // Given: an acknowledgement event from a member
        return new CommandAcknowledgedEvent(
                cmdId,
                teamId,
                member,
                Instant.now(clock)
        );
    }

    @Nested
    class SuccessfulScenarios {

        @Test
        void sagaStartsAndCompletesSuccessfully() {
            // Given: a command is issued
            CommandIssuedEvent issuedEvent = createCommandIssuedEvent(commandId, deadline);
            sagaManager.handleEvent(issuedEvent);

            // Then: the saga is started and is active
            Map<UUID, CommandSaga> activeSagas = sagaManager.getActiveSagas();
            assertEquals(1, activeSagas.size());
            CommandSaga saga = activeSagas.get(commandId);
            assertNotNull(saga);
            assertFalse(saga.isCompleted());

            // When: one member acknowledges
            sagaManager.handleEvent(createCommandAcknowledgedEvent(commandId, member1));

            // Then: saga is still pending
            assertFalse(saga.isCompleted());

            // When: all expected members acknowledge
            sagaManager.handleEvent(createCommandAcknowledgedEvent(commandId, member2));

            // Then: saga completes and is removed
            assertTrue(saga.isCompleted());
            assertFalse(sagaManager.getActiveSagas().containsKey(commandId));
        }

        @Test
        void sagaReplaysEvents() {
            // Given: a history of valid events
            CommandIssuedEvent issuedEvent = createCommandIssuedEvent(commandId, deadline);
            CommandAcknowledgedEvent ack1 = createCommandAcknowledgedEvent(commandId, member1);
            CommandAcknowledgedEvent ack2 = createCommandAcknowledgedEvent(commandId, member2);

            // When: events are replayed
            List<DomainEvent> pastEvents = List.of(issuedEvent, ack1, ack2);
            sagaManager.replayEvents(pastEvents);

            // Then: saga completes and is removed
            assertTrue(sagaManager.getActiveSagas().isEmpty());
        }
    }

    @Nested
    class FailureScenarios {

        @Test
        void ignoresEventsForOtherCommands() {
            // Given: an event with unrelated command ID
            UUID otherCommandId = UUID.randomUUID();
            CommandAcknowledgedEvent unrelatedAck = createCommandAcknowledgedEvent(otherCommandId, member1);

            // When: event is handled
            sagaManager.handleEvent(unrelatedAck);

            // Then: no saga is created
            assertTrue(sagaManager.getActiveSagas().isEmpty());
        }

        @Test
        void duplicateAcknowledgementsDoNotAffectSaga() {
            // Given: a saga started with a valid issued event
            CommandIssuedEvent issuedEvent = createCommandIssuedEvent(commandId, deadline);
            sagaManager.handleEvent(issuedEvent);
            CommandSaga saga = sagaManager.getActiveSagas().get(commandId);
            assertNotNull(saga);

            // When: same member acknowledges multiple times
            CommandAcknowledgedEvent ack = createCommandAcknowledgedEvent(commandId, member1);
            sagaManager.handleEvent(ack);
            sagaManager.handleEvent(ack);

            // Then: only one acknowledgment is counted
            assertEquals(1, saga.getAcknowledgedBy().size());
            assertFalse(saga.isCompleted());
        }

        @Test
        void missingExpectedAcknowledgerKeepsSagaPending() {
            // Given: saga started with only partial acknowledgements
            CommandIssuedEvent issuedEvent = createCommandIssuedEvent(commandId, deadline);
            sagaManager.handleEvent(issuedEvent);
            CommandSaga saga = sagaManager.getActiveSagas().get(commandId);
            assertNotNull(saga);

            // When: only one member acknowledges
            sagaManager.handleEvent(createCommandAcknowledgedEvent(commandId, member1));

            // Then: saga remains pending
            assertEquals(SagaStatus.PENDING, saga.getStatus());
            assertFalse(saga.isCompleted());
        }

        @Test
        void nullOrInvalidEventsAreIgnored() {
            // When: null or unknown event is handled
            sagaManager.handleEvent(null);

            DomainEvent unknownEvent = new DomainEvent() {
                @Override
                public UUID aggregateId() {
                    return UUID.randomUUID();
                }

                @Override
                public Instant occurredAt() {
                    return Instant.now(clock);
                }
            };
            sagaManager.handleEvent(unknownEvent);

            // Then: no saga is created or crash occurs
            assertTrue(sagaManager.getActiveSagas().isEmpty());
        }

        @Test
        void partialAcknowledgementBeyondDeadlineCompensatesSaga() {
            // Given: saga with past deadline
            Instant pastDeadline = Instant.now(clock).minusSeconds(1);
            CommandSaga saga = CommandSagaTestBuilder.builder()
                    .commandId(commandId)
                    .teamId(teamId)
                    .expectedAcknowledgers(expectedAcknowledgers)
                    .deadline(pastDeadline)
                    .clock(clock)
                    .build();

            // When: one member acknowledges
            saga.handle(createCommandAcknowledgedEvent(commandId, member1));

            // Then: saga is compensated
            assertEquals(SagaStatus.COMPENSATED, saga.getStatus());

            // When: another late acknowledgment arrives
            saga.handle(createCommandAcknowledgedEvent(commandId, member2));

            // Then: saga remains compensated
            assertEquals(SagaStatus.COMPENSATED, saga.getStatus());
        }

        @Test
        void emptyExpectedAcknowledgerSetThrowsException() {
            // When: creating saga with no expected acknowledgers
            Instant futureDeadline = Instant.now(clock).plusSeconds(3600);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    CommandSagaTestBuilder.builder()
                            .commandId(commandId)
                            .teamId(teamId)
                            .expectedAcknowledgers(Collections.emptySet())
                            .deadline(futureDeadline)
                            .clock(clock)
                            .build()
            );

            // Then: exception is thrown with correct message
            assertTrue(ex.getMessage().contains("expectedAcknowledgers must not be null or empty"));
        }

        @Test
        void replayingEventsWithMissingIntermediateEventsKeepsSagaIncomplete() {
            // Given: only an acknowledgment event, no issued event
            CommandAcknowledgedEvent ack1 = createCommandAcknowledgedEvent(commandId, member1);

            // When: replaying incomplete history
            sagaManager.replayEvents(List.of(ack1));

            // Then: saga is not reconstructed
            assertTrue(sagaManager.getActiveSagas().isEmpty());
        }
    }
}
