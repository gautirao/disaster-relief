package com.disasterrelief.commandcenter.saga;

import com.disasterrelief.commandcenter.domain.event.CommandAcknowledgedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CommandSagaTest {

    private UUID commandId;
    private UUID teamId;
    private UUID member1;
    private UUID member2;
    private Set<UUID> expectedAcknowledgers;
    private CommandSaga saga;

    @BeforeEach
    void setUp() {
        // Given: Common setup for all tests
        commandId = UUID.randomUUID();
        teamId = UUID.randomUUID();
        member1 = UUID.randomUUID();
        member2 = UUID.randomUUID();
        expectedAcknowledgers = Set.of(member1, member2);
        saga = new CommandSaga(commandId, teamId, expectedAcknowledgers);
    }

    @Nested
    class SuccessScenarios {

        @Test
        void initialState() {
            // Given: A newly created saga (setup already done)

            // When: We inspect the saga state

            // Then: It should be PENDING, no acknowledgments yet
            assertEquals(SagaStatus.PENDING, saga.getStatus());
            assertFalse(saga.isCompleted());
            assertTrue(saga.getAcknowledgedBy().isEmpty());
            assertEquals(commandId, saga.getCommandId());
            assertEquals(teamId, saga.getTeamId());
        }

        @Test
        void singleAcknowledgmentDoesNotCompleteSaga() {
            // Given: A new saga and one expected acknowledger

            // When: One acknowledgment event is handled
            saga.handle(new CommandAcknowledgedEvent(commandId, teamId, member1, Instant.now()));

            // Then: Saga status is still PENDING
            assertEquals(SagaStatus.PENDING, saga.getStatus());
            assertFalse(saga.isCompleted());
            assertEquals(Set.of(member1), saga.getAcknowledgedBy());
        }

        @Test
        void allAcknowledgmentsCompleteSaga() {
            // Given: A saga with two expected acknowledgers

            // When: Both acknowledgers send acknowledgment events
            saga.handle(new CommandAcknowledgedEvent(commandId, teamId, member1, Instant.now()));
            saga.handle(new CommandAcknowledgedEvent(commandId, teamId, member2, Instant.now()));

            // Then: Saga status is COMPLETED and all acknowledged
            assertEquals(SagaStatus.COMPLETED, saga.getStatus());
            assertTrue(saga.isCompleted());
            assertEquals(expectedAcknowledgers, saga.getAcknowledgedBy());
        }

        @Test
        void duplicateAcknowledgmentsDoNotAffectState() {
            // Given: A saga with expected acknowledgers

            // When: The same member acknowledges twice
            saga.handle(new CommandAcknowledgedEvent(commandId, teamId, member1, Instant.now()));
            saga.handle(new CommandAcknowledgedEvent(commandId, teamId, member1, Instant.now()));

            // Then: Saga status remains PENDING, acknowledgment counted once
            assertEquals(SagaStatus.PENDING, saga.getStatus());
            assertEquals(Set.of(member1), saga.getAcknowledgedBy());
        }

        @Test
        void toStringContainsImportantInfo() {
            // Given: A saga with an acknowledgment
            saga.handle(new CommandAcknowledgedEvent(commandId, teamId, member1, Instant.now()));

            // When: We call toString()
            String str = saga.toString();

            // Then: It should contain commandId, teamId, and member1 UUID strings
            assertTrue(str.contains(commandId.toString()));
            assertTrue(str.contains(teamId.toString()));
            assertTrue(str.contains(member1.toString()));
        }
    }

    @Nested
    class FailureAndEdgeCases {

        @Test
        void ignoreAcknowledgmentsForOtherCommands() {
            // Given: A saga for a specific commandId
            UUID otherCommandId = UUID.randomUUID();

            // When: An acknowledgment event comes for a different commandId
            saga.handle(new CommandAcknowledgedEvent(otherCommandId, teamId, member1, Instant.now()));

            // Then: Saga ignores it, remains PENDING with no acknowledgments
            assertEquals(SagaStatus.PENDING, saga.getStatus());
            assertTrue(saga.getAcknowledgedBy().isEmpty());
        }

        @Test
        void handleTimeoutTransitionsToFailedIfNotCompleted() {
            // Given: A saga that is still PENDING

            // When: Timeout occurs
            saga.handleTimeout();

            // Then: Saga status transitions to FAILED
            assertEquals(SagaStatus.FAILED, saga.getStatus());
            assertFalse(saga.isCompleted());
        }

        @Test
        void handleTimeoutDoesNothingIfAlreadyCompleted() {
            // Given: A saga that is COMPLETED
            saga.handle(new CommandAcknowledgedEvent(commandId, teamId, member1, Instant.now()));
            saga.handle(new CommandAcknowledgedEvent(commandId, teamId, member2, Instant.now()));
            assertTrue(saga.isCompleted());

            // When: Timeout occurs after completion
            saga.handleTimeout();

            // Then: Saga remains COMPLETED, no change
            assertEquals(SagaStatus.COMPLETED, saga.getStatus());
        }

        @Test
        void compensateDoesNotChangeStatus() {
            // Given: A saga in PENDING state

            // When: compensate is called (placeholder logic)
            saga.compensate();

            // Then: Saga status remains PENDING
            assertEquals(SagaStatus.PENDING, saga.getStatus());
        }
    }
}
