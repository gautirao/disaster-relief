package com.disasterrelief.commandcenter.saga;

import com.disasterrelief.commandcenter.persistence.PersistedEventRepository;
import com.disasterrelief.core.eventstore.PersistedEvent;
import com.disasterrelief.core.saga.CompensationHandler;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Fluent builder for CommandSaga used in unit tests.
 */
public class CommandSagaTestBuilder {

    private UUID commandId = UUID.randomUUID();
    private UUID teamId = UUID.randomUUID();
    private Set<UUID> expectedAcknowledgers = Set.of(UUID.randomUUID(), UUID.randomUUID());
    private Instant deadline = Instant.now().plusSeconds(3600);
    private Clock clock = Clock.systemUTC();
    private CompensationHandler<UUID> compensationHandler = (sagaId, reason) -> {};
    private PersistedEventRepository persistedEventRepository = new InMemoryRepoStub();

    public static CommandSagaTestBuilder builder() {
        return new CommandSagaTestBuilder();
    }

    public CommandSagaTestBuilder commandId(UUID commandId) {
        this.commandId = commandId;
        return this;
    }

    public CommandSagaTestBuilder teamId(UUID teamId) {
        this.teamId = teamId;
        return this;
    }

    public CommandSagaTestBuilder expectedAcknowledgers(Set<UUID> expectedAcknowledgers) {
        this.expectedAcknowledgers = expectedAcknowledgers;
        return this;
    }

    public CommandSagaTestBuilder deadline(Instant deadline) {
        this.deadline = deadline;
        return this;
    }

    public CommandSagaTestBuilder clock(Clock clock) {
        this.clock = clock;
        return this;
    }

    public CommandSagaTestBuilder compensationHandler(CompensationHandler<UUID> compensationHandler) {
        this.compensationHandler = compensationHandler;
        return this;
    }

    public CommandSagaTestBuilder persistedEventRepository(PersistedEventRepository repo) {
        this.persistedEventRepository = repo;
        return this;
    }

    public CommandSaga build() {
        return new CommandSaga(
                commandId,
                teamId,
                expectedAcknowledgers,
                deadline,
                compensationHandler,
                clock,
                persistedEventRepository
        );
    }

    /**
     * In-memory default stub repo to avoid nulls for tests that don’t assert event persistence.
     */
    private static class InMemoryRepoStub implements PersistedEventRepository {
        private final List<PersistedEvent> store = new CopyOnWriteArrayList<>();

        @Override
        public void save(PersistedEvent event) {
            store.add(event);
        }

        @Override
        public List<PersistedEvent> findBySagaId(UUID sagaId) {
            return store.stream()
                    .filter(e -> sagaId.equals(e.getSagaId()))
                    .toList();
        }
    }
}
