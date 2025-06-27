package com.disasterrelief.commandcenter.application;

import com.disasterrelief.commandcenter.domain.aggregate.CommandAggregate;
import com.disasterrelief.commandcenter.domain.command.SendCommandToTeamCommand;
import com.disasterrelief.commandcenter.domain.command.AcknowledgeCommandCommand;
import com.disasterrelief.commandcenter.domain.event.DomainEvent;
import com.disasterrelief.commandcenter.infrastructure.store.EventStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommandService {

    private final EventStore eventStore;

    public CommandService(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    public void sendCommand(SendCommandToTeamCommand command) {
        List<DomainEvent> pastEvents = eventStore.loadEvents(command.commandId());
        CommandAggregate aggregate = CommandAggregate.rehydrate(pastEvents);

        List<DomainEvent> newEvents = aggregate.handle(command);
        newEvents.forEach(eventStore::append);
    }

    public void acknowledgeCommand(AcknowledgeCommandCommand command) {
        List<DomainEvent> pastEvents = eventStore.loadEvents(command.commandId());
        CommandAggregate aggregate = CommandAggregate.rehydrate(pastEvents);

        List<DomainEvent> newEvents = aggregate.handle(command);
        newEvents.forEach(eventStore::append);
    }
}
