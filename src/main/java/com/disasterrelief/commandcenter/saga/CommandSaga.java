package com.disasterrelief.commandcenter.saga;

import com.disasterrelief.commandcenter.domain.event.CommandAcknowledgedEvent;
import com.disasterrelief.core.event.DomainEvent;
import com.disasterrelief.core.saga.Saga;

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

    private SagaStatus status = SagaStatus.PENDING;

    public CommandSaga(UUID commandId, UUID teamId, Set<UUID> expectedAcknowledgers, Instant deadline) {
        if (commandId == null) throw new IllegalArgumentException("commandId must not be null");
        if (teamId == null) throw new IllegalArgumentException("teamId must not be null");
        if (expectedAcknowledgers == null || expectedAcknowledgers.isEmpty())
            throw new IllegalArgumentException("expectedAcknowledgers must not be null or empty");
        if (deadline == null) throw new IllegalArgumentException("deadline must not be null");

        this.commandId = commandId;
        this.teamId = teamId;
        this.expectedAcknowledgers = Set.copyOf(expectedAcknowledgers);
        this.deadline = deadline;
    }

    @Override
    public UUID getId() {
        return commandId;
    }

    @Override
    public void handle(DomainEvent event) {
        // Ignore events not related to this saga's commandId
        if (event instanceof CommandAcknowledgedEvent ack) {
            if (!ack.commandId().equals(commandId)) {
                return;
            }
            // Only process if still pending
            if (status != SagaStatus.PENDING) {
                return;
            }

            acknowledgedBy.add(ack.memberId());

            if (acknowledgedBy.containsAll(expectedAcknowledgers)) {
                status = SagaStatus.COMPLETED;
            }
        }

        // Check for timeout on every event handling (or could be scheduled)
        checkTimeout();
    }

    private void checkTimeout() {
        if (status == SagaStatus.PENDING && Instant.now().isAfter(deadline)) {
            compensate();
        }
    }

    private void compensate() {
        status = SagaStatus.COMPENSATED;
        System.out.printf("Saga %s timed out. Compensation triggered.%n", commandId);
        // TODO: add compensation logic like rollback or alerts here
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
}
