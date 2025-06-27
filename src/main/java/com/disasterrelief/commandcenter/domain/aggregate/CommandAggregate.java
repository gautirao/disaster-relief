package com.disasterrelief.commandcenter.domain.aggregate;

import com.disasterrelief.commandcenter.domain.command.SendCommandToTeamCommand;
import com.disasterrelief.commandcenter.domain.command.AcknowledgeCommandCommand;
import com.disasterrelief.commandcenter.domain.event.*;
import com.disasterrelief.commandcenter.domain.valueobject.CommandStatus;

import java.time.Instant;
import java.util.*;

public class CommandAggregate {

    private UUID commandId;
    private UUID teamId;
    private String messageContent;
    private Instant deadline;
    private CommandStatus status;

    public static CommandAggregate rehydrate(List<DomainEvent> history) {
        CommandAggregate aggregate = new CommandAggregate();
        for (DomainEvent event : history) {
            aggregate.apply(event);
        }
        return aggregate;
    }

    private void apply(DomainEvent event) {
        if (event instanceof CommandIssuedEvent e) {
            this.commandId = e.commandId();
            this.teamId = e.teamId();
            this.messageContent = e.message().content();
            this.deadline = e.deadline();
            this.status = CommandStatus.ISSUED;
        } else if (event instanceof CommandAcknowledgedEvent) {
            this.status = CommandStatus.ACKNOWLEDGED;
        } else if (event instanceof CommandEscalatedEvent) {
            this.status = CommandStatus.ESCALATED;
        }
        // Add more event types as you expand the domain
    }

    // Command handlers, e.g.:
    public List<DomainEvent> handle(SendCommandToTeamCommand command) {
        if (this.status != null) {
            throw new IllegalStateException("Command already issued");
        }

        return List.of(new CommandIssuedEvent(
                command.commandId(),
                command.teamId(),
                command.message(),
                command.deadline(),
                Instant.now()
        ));
    }

    public List<DomainEvent> handle(AcknowledgeCommandCommand command) {
        if (this.status != CommandStatus.ISSUED) {
            throw new IllegalStateException("Command not in ISSUED state");
        }

        return List.of(new CommandAcknowledgedEvent(
                command.commandId(),
                command.teamId(),
                command.memberId(),
                command.acknowledgedAt()
        ));
    }
}
