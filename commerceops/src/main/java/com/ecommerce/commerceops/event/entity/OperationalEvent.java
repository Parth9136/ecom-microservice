package com.ecommerce.commerceops.event.entity;

import com.ecommerce.commerceops.event.dto.EventIngestionRequest;
import tools.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "operational_events",
        indexes = {
                @Index(
                        name = "idx_event_aggregate_id",
                        columnList = "aggregate_id"
                ),
                @Index(
                        name = "idx_event_occurred_at",
                        columnList = "occurred_at"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_operational_event_event_id",
                        columnNames = "event_id"
                )
        }
)
public class OperationalEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    @Column(name = "source_service", nullable = false, length = 100)
    private String sourceService;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private JsonNode payload;

    protected OperationalEvent() {
    }

    public static OperationalEvent from(EventIngestionRequest request) {

        OperationalEvent event = new OperationalEvent();

        event.eventId = request.eventId();
        event.eventType = request.eventType();
        event.sourceService = request.sourceService();
        event.aggregateType = request.aggregateType();
        event.aggregateId = request.aggregateId();
        event.occurredAt = request.occurredAt();
        event.payload = request.payload();

        return event;
    }

    @PrePersist
    protected void onCreate() {
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getSourceService() {
        return sourceService;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public JsonNode getPayload() {
        return payload;
    }
}
