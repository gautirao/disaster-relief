package com.disasterrelief.core.eventstore;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "persisted_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersistedEvent {

  @Id private UUID id;

  @Column(nullable = false)
  private UUID sagaId;

  @Column(nullable = false)
  private String eventType;

  @Lob
  @Column(nullable = false)
  private String eventPayload;

  @Column(nullable = false)
  private Instant createdAt;
}
