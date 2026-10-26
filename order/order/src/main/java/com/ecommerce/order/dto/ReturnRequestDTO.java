
package com.ecommerce.order.dto;

public record ReturnRequestDTO(
        Long orderId,
        Long orderItemId,
        String userId,
        Integer quantity,
        String reason
) {
}
