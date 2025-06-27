package com.disasterrelief.commandcenter.domain.command;

import java.time.Instant;
import java.util.UUID;

/**
 * Command issued by a team member to acknowledge a command.
 */
public record AcknowledgeCommandCommand(
        UUID commandId,
        UUID teamId,
        UUID memberId,
        Instant acknowledgedAt
) {}
