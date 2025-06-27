package com.disasterrelief.commandcenter.saga;

import com.disasterrelief.commandcenter.domain.event.CommandAcknowledgedEvent;
import com.disasterrelief.core.event.SagaCompensatedEvent;
import com.disasterrelief.core.event.DomainEvent;
import com.disasterrelief.core.saga.CompensationHandler;
import com.disasterrelief.core.saga.Saga;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CommandSaga implements Saga<UUID> {

    private final UUID commandId;
    private final UUID teamId;
    private final Set<UUID> expectedAcknowledgers;
    private final Set<UUID> acknowledgedBy = new HashSet<>();
    private final Instant deadline;
    private final CompensationHandler<UUID> compensationHandler;
    private final Clock clock;

    private SagaStatus status = SagaStatus.PENDING;
    private Instant compensationTime;
    private String compensationReason;

    // ✅ Primary constructor for full control (used in tests or advanced scenarios)
    public CommandSaga(UUID commandId,
                       UUID teamId,
                       Set<UUID> expectedAcknowledgers,
                       Instant deadline,
                       CompensationHandler<UUID> compensationHandler,
                       Clock clock) {
        if (commandId == null) throw new IllegalArgumentException("commandId must not be null");
        if (teamId == null) throw new IllegalArgumentException("teamId must not be null");
        if (expectedAcknowledgers == null || expectedAcknowledgers.isEmpty())
            throw new IllegalArgumentException("expectedAcknowledgers must not be null or empty");
        if (deadline == null) throw new IllegalArgumentException("deadline must not be null");
        if (compensationHandler == null) throw new IllegalArgumentException("compensationHandler must not be null");
        if (clock == null) throw new IllegalArgumentException("clock must not be null");

        this.commandId = commandId;
        this.teamId = teamId;
        this.expectedAcknowledgers = Set.copyOf(expectedAcknowledgers);
        this.deadline = deadline;
        this.compensationHandler = compensationHandler;
        this.clock = clock;
    }

    // ✅ Convenience constructor for production usage
    public CommandSaga(UUID commandId,
                       UUID teamId,
                       Set<UUID> expectedAcknowledgers,
                       Instant deadline,
                       CompensationHandler<UUID> compensationHandler) {
        this(commandId, teamId, expectedAcknowledgers, deadline, compensationHandler, Clock.systemUTC());
    }

    @Override
    public UUID getId() {
        return commandId;
    }

    @Override
    public void handle(DomainEvent event) {
        if (event == null) return;

        if (event instanceof CommandAcknowledgedEvent ack) {
            if (!ack.commandId().equals(commandId)) return;
            if (status == SagaStatus.COMPENSATED) return;
            if (status != SagaStatus.PENDING) return;

            acknowledgedBy.add(ack.memberId());

            if (acknowledgedBy.containsAll(expectedAcknowledgers)) {
                status = SagaStatus.COMPLETED;
            }
        }

        checkTimeout();
    }
    private void checkTimeout() {
        if (status == SagaStatus.PENDING && Instant.now(clock).isAfter(deadline)) {
            compensateDueToTimeout();
        }
    }

    private void compensateDueToTimeout() {
        compensate("Timeout reached before all acknowledgements.");
    }

    private void compensate(String reason) {
        if (status == SagaStatus.COMPENSATED) return;

        status = SagaStatus.COMPENSATED;
        compensationTime = Instant.now(clock);
        compensationReason = reason;
        compensationHandler.compensate(commandId, reason);

        // Optional: emit SagaCompensatedEvent
        SagaCompensatedEvent event = new SagaCompensatedEvent(commandId, compensationTime, reason);
        // publishEvent(event);
    }


    @Override
    public boolean isCompleted() {
        return status == SagaStatus.COMPLETED;
    }

    public SagaStatus getStatus() {
        return status;
    }

    public UUID getCommandId() {
        return commandId;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public Set<UUID> getAcknowledgedBy() {
        return Collections.unmodifiableSet(acknowledgedBy);
    }

    public Instant getDeadline() {
        return deadline;
    }

    public Instant getCompensationTime() {
        return compensationTime;
    }

    public String getCompensationReason() {
        return compensationReason;
    }
}
