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

    private Instant futureDeadline;
    private Instant pastDeadline;
    private Instant now;

    @BeforeEach
    void setup() {
        commandId = UUID.randomUUID();
        teamId = UUID.randomUUID();
        member1 = UUID.randomUUID();
        member2 = UUID.randomUUID();

        expectedAcknowledgers = Set.of(member1, member2);

        now = Instant.now();
        futureDeadline = now.plusSeconds(60 * 60); // 1 hour ahead
        pastDeadline = now.minusSeconds(60 * 60);  // 1 hour ago
    }

    @Nested
    class SuccessfulScenarios {

        private CommandSaga saga;

        @BeforeEach
        void initSaga() {
            saga = new CommandSaga(commandId, teamId, expectedAcknowledgers, futureDeadline);
        }

        @Test
        void sagaCompletesWhenAllMembersAcknowledge() {
            // Given: a new CommandSaga with expected acknowledgers

            // When: member1 acknowledges
            saga.handle(new CommandAcknowledgedEvent(commandId, teamId, member1, now));

            // Then: saga is still PENDING and member1 is recorded
            assertEquals(SagaStatus.PENDING, saga.getStatus());
            assertTrue(saga.getAcknowledgedBy().contains(member1));
            assertFalse(saga.isCompleted());

            // When: member2 acknowledges
            saga.handle(new CommandAcknowledgedEvent(commandId, teamId, member2, now));

            // Then: saga is COMPLETED and both members are acknowledged
            assertEquals(SagaStatus.COMPLETED, saga.getStatus());
            assertTrue(saga.isCompleted());
            assertTrue(saga.getAcknowledgedBy().containsAll(expectedAcknowledgers));
        }

        @Test
        void noCompensationIfAcknowledgedBeforeTimeout() {
            // Given: a saga with deadline in the future

            // When: all acknowledgments received before deadline
            saga.handle(new CommandAcknowledgedEvent(commandId, teamId, member1, now));
            saga.handle(new CommandAcknowledgedEvent(commandId, teamId, member2, now));

            // Then: saga is COMPLETED, no compensation triggered
            assertEquals(SagaStatus.COMPLETED, saga.getStatus());
        }
    }

    @Nested
    class UnsuccessfulScenarios {

        private CommandSaga saga;

        @BeforeEach
        void initSaga() {
            saga = new CommandSaga(commandId, teamId, expectedAcknowledgers, futureDeadline);
        }

        @Test
        void ignoreAcknowledgmentsForOtherCommands() {
            // Given: a saga for a specific commandId

            // When: an acknowledgment event comes for a different commandId
            UUID otherCommandId = UUID.randomUUID();
            saga.handle(new CommandAcknowledgedEvent(otherCommandId, teamId, member1, now));

            // Then: saga ignores it, remains PENDING with no acknowledgments
            assertEquals(SagaStatus.PENDING, saga.getStatus());
            assertTrue(saga.getAcknowledgedBy().isEmpty());
        }

        @Test
        void timeoutTriggersCompensation() {
            // Given: a saga with a deadline in the past (already expired)
            CommandSaga sagaWithExpiredDeadline = new CommandSaga(commandId, teamId, expectedAcknowledgers, pastDeadline);

            // When: any event is handled (or none), timeout is checked
            sagaWithExpiredDeadline.handle(new CommandAcknowledgedEvent(commandId, teamId, member1, now));

            // Then: saga is COMPENSATED due to timeout
            assertEquals(SagaStatus.COMPENSATED, sagaWithExpiredDeadline.getStatus());
            assertFalse(sagaWithExpiredDeadline.isCompleted());
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void duplicateAcknowledgmentFromSameMemberIgnored() {
            // Given: a saga with future deadline
            CommandSaga saga = new CommandSaga(commandId, teamId, expectedAcknowledgers, futureDeadline);

            // When: same member acknowledges multiple times
            saga.handle(new CommandAcknowledgedEvent(commandId, teamId, member1, now));
            saga.handle(new CommandAcknowledgedEvent(commandId, teamId, member1, now.plusSeconds(10)));

            // Then: acknowledgedBy set should contain member1 only once, status PENDING
            assertEquals(1, saga.getAcknowledgedBy().size());
            assertTrue(saga.getAcknowledgedBy().contains(member1));
            assertEquals(SagaStatus.PENDING, saga.getStatus());
        }

        @Test
        void acknowledgmentAfterCompletionDoesNotChangeStatus() {
            // Given: a saga that is already completed
            CommandSaga saga = new CommandSaga(commandId, teamId, expectedAcknowledgers, futureDeadline);
            saga.handle(new CommandAcknowledgedEvent(commandId, teamId, member1, now));
            saga.handle(new CommandAcknowledgedEvent(commandId, teamId, member2, now));
            assertEquals(SagaStatus.COMPLETED, saga.getStatus());

            // When: an acknowledgment comes in after completion (should be ignored)
            saga.handle(new CommandAcknowledgedEvent(commandId, teamId, UUID.randomUUID(), now.plusSeconds(10)));

            // Then: saga status remains COMPLETED and acknowledgedBy unchanged
            assertEquals(SagaStatus.COMPLETED, saga.getStatus());
            assertEquals(expectedAcknowledgers.size(), saga.getAcknowledgedBy().size());
        }

        @Test
        void createSagaWithEmptyAcknowledgersThrows() {
            // Given: an empty expectedAcknowledgers set
            Set<UUID> emptySet = Set.of();

            // Then: saga creation throws IllegalArgumentException
            assertThrows(IllegalArgumentException.class, () ->
                    new CommandSaga(commandId, teamId, emptySet, futureDeadline));
        }

        @Test
        void deadlineExactlyNowTriggersTimeout() {
            // Given: saga with deadline exactly now
            CommandSaga saga = new CommandSaga(commandId, teamId, expectedAcknowledgers, now);

            // When: handle any acknowledgment
            saga.handle(new CommandAcknowledgedEvent(commandId, teamId, member1, now));

            // Then: saga is COMPENSATED due to deadline reached
            assertEquals(SagaStatus.COMPENSATED, saga.getStatus());
        }

        @Test
        void handleNullOrUnknownEventTypeIgnored() {
            // Given: a saga with future deadline
            CommandSaga saga = new CommandSaga(commandId, teamId, expectedAcknowledgers, futureDeadline);

            // When: handling null event or unknown event class
            saga.handle(null);

            // Unknown event class
            class UnknownEvent implements com.disasterrelief.core.event.DomainEvent {
                @Override public java.util.UUID aggregateId() { return commandId; }
                @Override public java.time.Instant occurredAt() { return now; }
            }
            saga.handle(new UnknownEvent());

            // Then: saga status and acknowledgers unchanged
            assertEquals(SagaStatus.PENDING, saga.getStatus());
            assertTrue(saga.getAcknowledgedBy().isEmpty());
        }
    }
}
