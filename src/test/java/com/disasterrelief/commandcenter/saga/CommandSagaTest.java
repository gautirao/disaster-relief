package com.disasterrelief.commandcenter.saga;

import com.disasterrelief.commandcenter.domain.event.CommandAcknowledgedEvent;
import com.disasterrelief.core.saga.CompensationHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CommandSagaTest {

    private Instant now;
    private UUID commandId;
    private UUID teamId;
    private UUID member1;
    private UUID member2;
    private Set<UUID> expectedAcknowledgers;
    private Clock fixedClock;

    @BeforeEach
    void setup() {
        now = Instant.parse("2025-06-27T10:00:00Z");
        fixedClock = Clock.fixed(now, ZoneOffset.UTC);

        commandId = UUID.randomUUID();
        teamId = UUID.randomUUID();
        member1 = UUID.randomUUID();
        member2 = UUID.randomUUID();
        expectedAcknowledgers = Set.of(member1, member2);
    }

    @Nested
    class SuccessfulScenarios {

        @Test
        void sagaCompletesWhenAllMembersAcknowledge() {
            var saga = CommandSagaTestBuilder.builder()
                    .commandId(commandId)
                    .teamId(teamId)
                    .expectedAcknowledgers(expectedAcknowledgers)
                    .deadline(now.plusSeconds(3600))
                    .clock(fixedClock)
                    .build();

            saga.handle(new CommandAcknowledgedEvent(commandId, teamId, member1, now));
            assertThat(saga.getStatus()).isEqualTo(SagaStatus.PENDING);

            saga.handle(new CommandAcknowledgedEvent(commandId, teamId, member2, now));
            assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPLETED);
            assertThat(saga.getAcknowledgedBy()).containsExactlyInAnyOrder(member1, member2);
        }
    }

    @Nested
    class CompensationTests {

        @Test
        void timeoutTriggersCompensation() {
            @SuppressWarnings("unchecked")
            CompensationHandler<UUID> mockHandler = (CompensationHandler<UUID>) mock(CompensationHandler.class);

            var saga = CommandSagaTestBuilder.builder()
                    .commandId(commandId)
                    .teamId(teamId)
                    .expectedAcknowledgers(expectedAcknowledgers)
                    .deadline(now.minusSeconds(60))
                    .clock(fixedClock)
                    .compensationHandler(mockHandler)
                    .build();

            saga.handle(new CommandAcknowledgedEvent(commandId, teamId, member1, now));

            assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPENSATED);
            verify(mockHandler).compensate(eq(saga.getId()), any());
        }
    }

    @Nested
    class ConcurrencyTest {
        @Test
        void concurrentAcknowledgmentsWorkCorrectly() throws InterruptedException {
            CommandSaga saga = CommandSagaTestBuilder.builder()
                    .expectedAcknowledgers(expectedAcknowledgers)
                    .clock(fixedClock)
                    .build();

            try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
                executor.submit(() -> saga.handle(new CommandAcknowledgedEvent(saga.getCommandId(), saga.getTeamId(), member1, now)));
                executor.submit(() -> saga.handle(new CommandAcknowledgedEvent(saga.getCommandId(), saga.getTeamId(), member2, now)));
                executor.shutdown();
                boolean terminated = executor.awaitTermination(1, TimeUnit.SECONDS);
                if (!terminated) {
                    executor.shutdownNow();
                }
            }

            assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPLETED);
        }
    }

    @Nested
    class ParameterizedAcknowledgers {
        @ParameterizedTest
        @ValueSource(ints = {1, 3, 5})
        void sagaCompletesWithDifferentAcknowledgerCounts(int count) {
            Set<UUID> members = new HashSet<>();
            for (int i = 0; i < count; i++) {
                members.add(UUID.randomUUID());
            }

            var saga = CommandSagaTestBuilder.builder()
                    .expectedAcknowledgers(Set.copyOf(members))
                    .clock(fixedClock)
                    .build();

            members.forEach(member ->
                    saga.handle(new CommandAcknowledgedEvent(saga.getCommandId(), saga.getTeamId(), member, now)));

            assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPLETED);
        }
    }
}
