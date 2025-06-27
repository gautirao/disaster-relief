package com.disasterrelief.core.saga;

import com.disasterrelief.core.event.DomainEvent;

import java.util.UUID;

public interface Saga<ID> {

    /**
     * Returns the unique identifier for this Saga instance.
     */
    ID getId();

    /**
     * Handle a domain event to update the Saga's state.
     * This method is called for both new and replayed events.
     */
    void handle(DomainEvent event);

    /**
     * Returns true if the Saga is complete and no longer needs to process events.
     */
    boolean isCompleted();
}
