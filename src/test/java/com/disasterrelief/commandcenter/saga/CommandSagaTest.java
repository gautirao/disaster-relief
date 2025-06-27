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
    }

    @Nested
    class ConcurrencyTest {

        @Test
        void concurrentAcknowledgementsDoNotCorruptState() throws InterruptedException {
            CommandSaga saga = newSagaWith(expectedAcknowledgers);

            try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
                executor.submit(() -> saga.handle(new CommandAcknowledgedEvent(saga.getCommandId(), saga.getTeamId(), member1, now)));
                executor.submit(() -> saga.handle(new CommandAcknowledgedEvent(saga.getCommandId(), saga.getTeamId(), member2, now)));
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
}
