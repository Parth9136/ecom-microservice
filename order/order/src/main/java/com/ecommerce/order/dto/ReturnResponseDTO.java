
package com.ecommerce.order.dto;

import com.ecommerce.order.models.ReturnStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReturnResponseDTO(
        Long returnId,
        Long orderId,
        Long orderItemId,
        Integer quantity,
        String reason,
        BigDecimal requestedRefundAmount,
        ReturnStatus status,
        LocalDateTime createdAt
) {
}
