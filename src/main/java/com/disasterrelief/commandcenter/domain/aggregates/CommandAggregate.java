package com.disasterrelief.commandcenter.domain.aggregates;

import com.disasterrelief.commandcenter.domain.command.AcknowledgeCommandCommand;
import com.disasterrelief.commandcenter.domain.command.SendCommandToTeamCommand;
import com.disasterrelief.commandcenter.domain.event.CommandAcknowledgedEvent;
import com.disasterrelief.commandcenter.domain.event.CommandIssuedEvent;
import com.disasterrelief.commandcenter.domain.event.DomainEvent;
import com.disasterrelief.commandcenter.domain.valueobject.CommandStatus;
import com.disasterrelief.commandcenter.domain.valueobject.Message;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class CommandAggregate {
    private UUID commandId;
    private UUID teamId;
    private Message message;
    private Instant deadline;
    private CommandStatus status;

    public List<DomainEvent> handle(SendCommandToTeamCommand cmd) {
        if (cmd.deadline().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Deadline must be in the future");
        }

        return List.of(new CommandIssuedEvent(
                cmd.commandId(),
                cmd.teamId(),
                cmd.message(),
                cmd.deadline(),
                Instant.now()
        ));
    }

    public void apply(CommandIssuedEvent event) {
        this.commandId = event.commandId();
        this.teamId = event.teamId();
        this.message = event.message();
        this.deadline = event.deadline();
        this.status = CommandStatus.ISSUED;
    }

    public List<DomainEvent> handle(AcknowledgeCommandCommand ack) {
        if (status != CommandStatus.ISSUED) {
            throw new IllegalStateException("Cannot acknowledge unless issued");
        }

        return List.of(new CommandAcknowledgedEvent(commandId, teamId, ack.memberId(), Instant.now()));
    }

    public void apply(CommandAcknowledgedEvent event) {
        this.status = CommandStatus.ACKNOWLEDGED;
    }
}
