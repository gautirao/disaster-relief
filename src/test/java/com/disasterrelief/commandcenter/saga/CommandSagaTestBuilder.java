package com.disasterrelief.commandcenter.saga;

import com.disasterrelief.core.saga.CompensationHandler;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public class CommandSagaTestBuilder {

    private UUID commandId = UUID.randomUUID();
    private UUID teamId = UUID.randomUUID();
    private Set<UUID> expectedAcknowledgers = Set.of(UUID.randomUUID(), UUID.randomUUID());
    private Instant deadline = Instant.now().plusSeconds(3600);
    private Clock clock = Clock.systemUTC();
    private CompensationHandler<UUID> compensationHandler = (sagaId, reason) -> {};

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

    public CommandSagaTestBuilder compensationHandler(CompensationHandler<UUID> handler) {
        this.compensationHandler = handler;
        return this;
    }

    public CommandSaga build() {
        return new CommandSaga(commandId, teamId, expectedAcknowledgers, deadline, compensationHandler,clock);
    }
}
