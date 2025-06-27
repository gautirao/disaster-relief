package com.disasterrelief.commandcenter.domain.valueobject;

import java.time.Instant;
import java.util.UUID;

public record Message(String content, UUID senderId, Instant timestamp) {
    public Message {
        if (content == null || content.isBlank()) throw new IllegalArgumentException("Message content required");
        if (senderId == null) throw new IllegalArgumentException("Sender ID required");
        if (timestamp == null) throw new IllegalArgumentException("Timestamp required");
    }
}
