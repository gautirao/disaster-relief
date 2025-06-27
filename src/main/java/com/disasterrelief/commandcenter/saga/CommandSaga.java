package com.disasterrelief.commandcenter.saga;

import com.disasterrelief.commandcenter.domain.event.CommandAcknowledgedEvent;
import com.disasterrelief.core.event.DomainEvent;
import com.disasterrelief.core.saga.Saga;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Saga that tracks acknowledgements for a command issued to a team.
 * It completes once all expected members have acknowledged the command.
 */
public class CommandSaga implements Saga<UUID> {

    private final UUID commandId;
    private final UUID teamId;
    private final Set<UUID> expectedAcknowledgers;
    private final Set<UUID> acknowledgedBy = new HashSet<>();

    private SagaStatus status = SagaStatus.PENDING;

    /**
     * Creates a new CommandSaga instance.
     * @param commandId unique identifier of the command this saga tracks
     * @param teamId unique identifier of the team receiving the command
     * @param expectedAcknowledgers set of member IDs expected to acknowledge the command
     * @throws IllegalArgumentException if any argument is invalid
     */
    public CommandSaga(UUID commandId, UUID teamId, Set<UUID> expectedAcknowledgers) {
        if (commandId == null) throw new IllegalArgumentException("commandId must not be null");
        if (teamId == null) throw new IllegalArgumentException("teamId must not be null");
        if (expectedAcknowledgers == null || expectedAcknowledgers.isEmpty())
            throw new IllegalArgumentException("expectedAcknowledgers must not be null or empty");

        this.commandId = commandId;
        this.teamId = teamId;
        this.expectedAcknowledgers = Set.copyOf(expectedAcknowledgers);
    }

    @Override
    public UUID getId() {
        return commandId;
    }

    /**
     * Handles incoming domain events relevant to this saga.
     * @param event domain event to process
     */
    @Override
    public void handle(DomainEvent event) {
        if (event instanceof CommandAcknowledgedEvent ack && ack.commandId().equals(commandId)) {
            if (acknowledgedBy.add(ack.memberId())) {
                // Log state change if desired
                System.out.printf("Member %s acknowledged command %s%n", ack.memberId(), commandId);
            }

            if (acknowledgedBy.containsAll(expectedAcknowledgers)) {
                status = SagaStatus.COMPLETED;
                System.out.printf("CommandSaga %s is COMPLETED%n", commandId);
            }
        }

        // TODO: Add handlers for timeouts, failure compensation events, etc.
    }

    /**
     * Returns whether this saga has completed processing all acknowledgements.
     * @return true if all expected acknowledgements received, false otherwise
     */
    @Override
    public boolean isCompleted() {
        return status == SagaStatus.COMPLETED;
    }

    /**
     * Gets the current status of the saga.
     * @return saga status (e.g., PENDING, COMPLETED)
     */
    public SagaStatus getStatus() {
        return status;
    }

    /**
     * Gets the command ID this saga tracks.
     * @return command UUID
     */
    public UUID getCommandId() {
        return commandId;
    }

    /**
     * Gets the team ID this saga tracks.
     * @return team UUID
     */
    public UUID getTeamId() {
        return teamId;
    }

    /**
     * Returns an unmodifiable set of member IDs who have acknowledged so far.
     * @return set of acknowledged member IDs
     */
    public Set<UUID> getAcknowledgedBy() {
        return Set.copyOf(acknowledgedBy);
    }

    @Override
    public String toString() {
        return "CommandSaga{" +
                "commandId=" + commandId +
                ", teamId=" + teamId +
                ", expectedAcknowledgers=" + expectedAcknowledgers +
                ", acknowledgedBy=" + acknowledgedBy +
                ", status=" + status +
                '}';
    }

    // Future: Add methods to handle timeouts or failure compensation

    /**
     * Placeholder to trigger timeout processing.
     */
    public void handleTimeout() {
        if (status != SagaStatus.COMPLETED) {
            status = SagaStatus.FAILED;
            System.out.printf("CommandSaga %s timed out and marked as FAILED%n", commandId);
            // TODO: Trigger compensation, alerts, etc.
        }
    }

    /**
     * Placeholder to trigger failure compensation logic.
     */
    public void compensate() {
        System.out.printf("CommandSaga %s performing compensation%n", commandId);
        // TODO: Implement compensation logic to rollback side effects or notify
    }
}
