package com.disasterrelief.core.saga;

public interface CompensationHandler<Id> {
    void compensate(Id sagaId, String reason);
}
