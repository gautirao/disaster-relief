package com.disasterrelief.core.eventstore;

import com.disasterrelief.core.event.DomainEvent;

import java.util.*;

public interface EventStore {
    void append(DomainEvent event);
    List<DomainEvent> readAll();
    List<DomainEvent> readByAggregateId(UUID aggregateId);

}
