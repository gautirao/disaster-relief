package com.disasterrelief.commandcenter.domain.command;

import java.time.Instant;
import java.util.UUID;

/**
 * Command issued by a team member to acknowledge a command.
 */
import java.time.Instant;
import java.util.UUID;

public record AcknowledgeCommandCommand(
        UUID commandId,
        UUID teamId,
        UUID memberId,
        Instant acknowledgedAt
) {
    public AcknowledgeCommandCommand {
        if (commandId == null) {
            throw new IllegalArgumentException("commandId must not be null");
        }
        if (teamId == null) {
            throw new IllegalArgumentException("teamId must not be null");
        }
        if (memberId == null) {
            throw new IllegalArgumentException("memberId must not be null");
        }
        if (acknowledgedAt == null) {
            throw new IllegalArgumentException("acknowledgedAt must not be null");
        }
        if (acknowledgedAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException("acknowledgedAt cannot be in the future");
        }
    }
}

