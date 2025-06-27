package com.disasterrelief.commandcenter.domain.command;

import com.disasterrelief.commandcenter.domain.valueobject.Message;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Command to issue a new command to a team.
 */
public record SendCommandToTeamCommand(
        UUID commandId,
        UUID teamId,
        Message message,
        Instant deadline,
        UUID issuedBy,
        Set<UUID> expectedAcknowledgerIds
) {
    public SendCommandToTeamCommand {
        Objects.requireNonNull(commandId, "commandId must not be null");
        Objects.requireNonNull(teamId, "teamId must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(deadline, "deadline must not be null");
        Objects.requireNonNull(issuedBy, "issuedBy must not be null");
        if (expectedAcknowledgerIds == null || expectedAcknowledgerIds.isEmpty()) {
            throw new IllegalArgumentException("expectedAcknowledgerIds must not be null or empty");
        }
    }
}
