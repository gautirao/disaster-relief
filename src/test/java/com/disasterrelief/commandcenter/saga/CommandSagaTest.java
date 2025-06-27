package com.disasterrelief.commandcenter.saga;

import com.disasterrelief.commandcenter.domain.event.CommandAcknowledgedEvent;
import com.disasterrelief.commandcenter.persistence.InMemoryPersistedEventRepository;
import com.disasterrelief.commandcenter.persistence.PersistedEventRepository;
import com.disasterrelief.core.event.DomainEvent;
import com.disasterrelief.core.saga.CompensationHandler;
import com.disasterrelief.util.EventSerializationUtil;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

class CommandSagaTest {

    private Instant now;
    private Clock fixedClock;
    private UUID member1;
    private UUID member2;
    private Set<UUID> expectedAcknowledgers;
    private PersistedEventRepository eventStore;

    @BeforeEach
    void setup() {
        now = Instant.parse("2025-06-27T10:00:00Z");
        fixedClock = Clock.fixed(now, ZoneOffset.UTC);
        member1 = UUID.randomUUID();
        member2 = UUID.randomUUID();
        expectedAcknowledgers = Set.of(member1, member2);
        eventStore = new InMemoryPersistedEventRepository();
    }

    private CommandSaga newSagaWith(Set<UUID> acknowledgers) {
        return CommandSagaTestBuilder.builder()
                .expectedAcknowledgers(acknowledgers)
                .clock(fixedClock)
                .persistedEventRepository(eventStore)
                .build();
    }

    @Nested
    class SuccessfulScenarios {

        @Test
        void completesWhenAllExpectedMembersAcknowledge() {
            var saga = newSagaWith(expectedAcknowledgers);

            saga.handle(new CommandAcknowledgedEvent(saga.getCommandId(), saga.getTeamId(), member1, now));
            assertThat(saga.getStatus()).isEqualTo(SagaStatus.PENDING);

            saga.handle(new CommandAcknowledgedEvent(saga.getCommandId(), saga.getTeamId(), member2, now));
            assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPLETED);
            assertThat(saga.getAcknowledgedBy()).containsExactlyInAnyOrder(member1, member2);
        }

        @Test
        void ignoresDuplicateAcknowledgementFromSameMember() {
            var saga = newSagaWith(expectedAcknowledgers);

            saga.handle(new CommandAcknowledgedEvent(saga.getCommandId(), saga.getTeamId(), member1, now));
            saga.handle(new CommandAcknowledgedEvent(saga.getCommandId(), saga.getTeamId(), member1, now)); // duplicate
            saga.handle(new CommandAcknowledgedEvent(saga.getCommandId(), saga.getTeamId(), member2, now));

            assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPLETED);
            assertThat(saga.getAcknowledgedBy()).containsExactlyInAnyOrder(member1, member2);
        }

        @Test
        void ignoresAcknowledgementFromUnexpectedMember() {
            var saga = newSagaWith(expectedAcknowledgers);

            UUID unexpectedMember = UUID.randomUUID();
            saga.handle(new CommandAcknowledgedEvent(saga.getCommandId(), saga.getTeamId(), unexpectedMember, now));
            saga.handle(new CommandAcknowledgedEvent(saga.getCommandId(), saga.getTeamId(), member1, now));
            saga.handle(new CommandAcknowledgedEvent(saga.getCommandId(), saga.getTeamId(), member2, now));

            assertThat(saga.getAcknowledgedBy()).doesNotContain(unexpectedMember);
            assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPLETED);
        }

        @Test
        void ignoresAcknowledgementAfterCompletion() {
            var saga = newSagaWith(expectedAcknowledgers);

            // Complete saga by acknowledging all expected members
            expectedAcknowledgers.forEach(member ->
                    saga.handle(new CommandAcknowledgedEvent(saga.getCommandId(), saga.getTeamId(), member, now)));

            assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPLETED);

            // Attempt acknowledgement after completion from a new member
            UUID newMember = UUID.randomUUID();
            saga.handle(new CommandAcknowledgedEvent(saga.getCommandId(), saga.getTeamId(), newMember, now));

            assertThat(saga.getAcknowledgedBy()).doesNotContain(newMember);
            assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPLETED);
        }
    }

    @Nested
    class CompensationTests {
        static class TestCompensationHandler implements CompensationHandler<UUID> {
            boolean called = false;
            UUID compensatedId;
            String compensatedReason;

            @Override
            public void compensate(UUID sagaId, String reason) {
                called = true;
                compensatedId = sagaId;
                compensatedReason = reason;
            }
        }

        @Test
        void compensatesOnTimeoutEvenIfSomeAcknowledged() {
            var handler = new TestCompensationHandler();

            var saga = CommandSagaTestBuilder.builder()
                    .expectedAcknowledgers(expectedAcknowledgers)
                    .deadline(now.minusSeconds(60))
                    .clock(fixedClock)
                    .compensationHandler(handler)
                    .persistedEventRepository(eventStore)
                    .build();

            saga.handle(new CommandAcknowledgedEvent(saga.getCommandId(), saga.getTeamId(), member1, now));

            assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPENSATED);
            assertThat(handler.called).isTrue();
            assertThat(handler.compensatedId).isEqualTo(saga.getId());
            assertThat(handler.compensatedReason).contains("Timeout");
        }

        @Test
        void ignoresAcknowledgementAfterCompensation() {
            var handler = new TestCompensationHandler();

            var saga = CommandSagaTestBuilder.builder()
                    .expectedAcknowledgers(expectedAcknowledgers)
                    .deadline(now.minusSeconds(10))
                    .compensationHandler(handler)
                    .clock(fixedClock)
                    .persistedEventRepository(eventStore)
                    .build();

            // Trigger compensation by sending one ack after deadline
            saga.handle(new CommandAcknowledgedEvent(saga.getCommandId(), saga.getTeamId(), member1, now));
            assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPENSATED);

            // Try to acknowledge after compensation
            saga.handle(new CommandAcknowledgedEvent(saga.getCommandId(), saga.getTeamId(), member2, now));
            assertThat(saga.getAcknowledgedBy()).doesNotContain(member2);
            assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPENSATED);
            assertThat(handler.called).isTrue();
        }

        @Test
        void partialAcknowledgementsFollowedByTimeoutCompensation() {
            var handler = new TestCompensationHandler();

            var saga = CommandSagaTestBuilder.builder()
                    .expectedAcknowledgers(expectedAcknowledgers)
                    .deadline(now.minusSeconds(1))  // deadline already passed
                    .compensationHandler(handler)
                    .clock(fixedClock)
                    .persistedEventRepository(eventStore)
                    .build();

            // One member acknowledges before compensation triggers
            saga.handle(new CommandAcknowledgedEvent(saga.getCommandId(), saga.getTeamId(), member1, now));

            // Should compensate immediately due to timeout
            assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPENSATED);
            assertThat(saga.getAcknowledgedBy()).contains(member1);
            assertThat(handler.called).isTrue();
            assertThat(handler.compensatedId).isEqualTo(saga.getId());
        }
    }

    @Nested
    class ConcurrencyTest {

        @Test
        void concurrentAcknowledgementsDoNotCorruptState() throws InterruptedException {
            CommandSaga saga = newSagaWith(expectedAcknowledgers);

            try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
                Future<?> f1 = executor.submit(() -> saga.handle(new CommandAcknowledgedEvent(saga.getCommandId(), saga.getTeamId(), member1, now)));
                Future<?> f2 = executor.submit(() -> saga.handle(new CommandAcknowledgedEvent(saga.getCommandId(), saga.getTeamId(), member2, now)));
                f1.get();
                f2.get();
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }

            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(saga.getAcknowledgedBy()).containsExactlyInAnyOrder(member1, member2);
            softly.assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPLETED);
            softly.assertAll();
        }
    }

    @Nested
    class ParameterizedAcknowledgers {

        static List<Integer> counts() {
            return List.of(1, 3, 5, 10);
        }

        @ParameterizedTest
        @MethodSource("counts")
        void completesForVariousExpectedMemberCounts(int count) {
            Set<UUID> members = new HashSet<>();
            for (int i = 0; i < count; i++) {
                members.add(UUID.randomUUID());
            }

            var saga = newSagaWith(members);

            members.forEach(member ->
                    saga.handle(new CommandAcknowledgedEvent(saga.getCommandId(), saga.getTeamId(), member, now)));

            assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPLETED);
        }
    }

    @Nested
    class EventReplayTest {

        @Test
        void stateCanBeRehydratedFromPersistedEvents() {
            UUID commandId = UUID.randomUUID();
            UUID teamId = UUID.randomUUID();

            CommandSaga original = CommandSagaTestBuilder.builder()
                    .commandId(commandId)
                    .teamId(teamId)
                    .expectedAcknowledgers(expectedAcknowledgers)
                    .clock(fixedClock)
                    .persistedEventRepository(eventStore)
                    .build();

            original.handle(new CommandAcknowledgedEvent(commandId, teamId, member1, now));
            original.handle(new CommandAcknowledgedEvent(commandId, teamId, member2, now));

            List<DomainEvent> persistedEvents = eventStore.findBySagaId(commandId).stream()
                    .map(persisted -> {
                        try {
                            Class<?> clazz = Class.forName(persisted.getEventType());
                            return (DomainEvent) EventSerializationUtil.deserialize(persisted.getEventPayload(), clazz);
                        } catch (Exception e) {
                            throw new RuntimeException("Deserialization failed", e);
                        }
                    })
                    .toList();

            CommandSaga replayed = CommandSagaTestBuilder.builder()
                    .commandId(commandId)
                    .teamId(teamId)
                    .expectedAcknowledgers(expectedAcknowledgers)
                    .clock(fixedClock)
                    .persistedEventRepository(eventStore)
                    .build();

            persistedEvents.forEach(replayed::handle);

            assertThat(replayed.getStatus()).isEqualTo(SagaStatus.COMPLETED);
            assertThat(replayed.getAcknowledgedBy()).containsExactlyInAnyOrder(member1, member2);
        }
    }

    @Nested
    class FailureModesTest {

        PersistedEventRepository failingEventStore;
        CommandSaga saga;

        @BeforeEach
        void setup() {
            failingEventStore = mock(PersistedEventRepository.class);
            doThrow(new RuntimeException("Persistence failed"))
                    .when(failingEventStore)
                    .save(any());

            saga = CommandSagaTestBuilder.builder()
                    .expectedAcknowledgers(expectedAcknowledgers)
                    .clock(fixedClock)
                    .persistedEventRepository(failingEventStore)
                    .build();
        }

        @Test
        void throwsExceptionWhenPersistenceFails() {
            CommandAcknowledgedEvent event = new CommandAcknowledgedEvent(saga.getCommandId(), saga.getTeamId(), member1, now);

            RuntimeException ex = assertThrows(RuntimeException.class, () -> saga.handle(event));
            assertThat(ex).hasMessageContaining("Failed to persist event");
        }

        @Test
        void handlesNullEventGracefully() {
            saga = newSagaWith(expectedAcknowledgers);

            // Depending on your saga implementation, either ignore or throw an exception on null
            assertDoesNotThrow(() -> saga.handle(null));
            // Or if you want to check for IllegalArgumentException:
            // assertThrows(IllegalArgumentException.class, () -> saga.handle(null));
        }

        @Test
        void ignoresUnknownEventTypes() {
            saga = newSagaWith(expectedAcknowledgers);

            // Create an unknown event type implementing DomainEvent
            DomainEvent unknownEvent = new UnknownEvent(saga.getId(),now);

            // Either ignore or throw, depending on your saga's design
            assertDoesNotThrow(() -> saga.handle(unknownEvent));
            // Or assertThrows(IllegalArgumentException.class, () -> saga.handle(unknownEvent));
        }

        // need this for jackson
        public static class UnknownEvent implements DomainEvent {
            private final UUID sagaId;
            private final Instant occurredAt;

            public UnknownEvent(UUID sagaId, Instant occurredAt) {
                this.sagaId = sagaId;
                this.occurredAt = occurredAt;
            }

            @Override
            public UUID aggregateId() {
                return sagaId;
            }

            @Override
            public Instant occurredAt() {
                return occurredAt;
            }

            // Optional: Add getters if needed for serialization
            public UUID getSagaIdField() {
                return sagaId;
            }

            public Instant getOccurredAtField() {
                return occurredAt;
            }


        }

    }
}
