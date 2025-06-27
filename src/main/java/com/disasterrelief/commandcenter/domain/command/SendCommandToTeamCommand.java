package com.disasterrelief.commandcenter.domain.command;

import com.disasterrelief.commandcenter.domain.valueobject.Message;

import java.time.Instant;
import java.util.UUID;

/**
 * Command to issue a new command to a team.
 */
public record SendCommandToTeamCommand(
        UUID commandId,
        UUID teamId,
        Message message,
        Instant deadline

){
    public SendCommandToTeamCommand {
        if (commandId == null) {
            throw new IllegalArgumentException("commandId must not be null");
        }
        if (teamId == null) {
            throw new IllegalArgumentException("teamId must not be null");
        }
        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }
        if (deadline == null) {
            throw new IllegalArgumentException("deadline must not be null");
        }
        if (deadline.isBefore(Instant.now())) {
            throw new IllegalArgumentException("deadline must be in the future");
        }
    }
}