package com.disasterrelief.commandcenter.infrastructure.store;

import com.disasterrelief.core.event.DomainEvent;

import java.util.*;

public interface EventStore {
    List<DomainEvent> loadEvents(UUID aggregateId);
    void append(DomainEvent event);
    List<DomainEvent> getAllEvents(); // <-- Add this if not present

}
