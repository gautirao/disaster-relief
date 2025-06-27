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

) {}
