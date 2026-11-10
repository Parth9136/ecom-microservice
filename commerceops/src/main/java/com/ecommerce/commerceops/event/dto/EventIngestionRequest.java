package com.ecommerce.commerceops.event.dto;

import com.ecommerce.commerceops.event.entity.EventType;
import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record EventIngestionRequest(

        @NotBlank
        @Size(max = 100)
        String eventId,

        @NotNull
        EventType eventType,

        @NotBlank
        @Size(max = 100)
        String sourceService,

        @NotBlank
        @Size(max = 50)
        String aggregateType,

        @NotBlank
        @Size(max = 100)
        String aggregateId,

        @NotNull
        Instant occurredAt,

        @NotNull
        JsonNode payload

) {
}
