
package com.ecommerce.order.services;

import com.ecommerce.order.dto.ReturnRequestDTO;
import com.ecommerce.order.dto.ReturnResponseDTO;
import com.ecommerce.order.models.*;
import com.ecommerce.order.repositories.OrderRepository;
import com.ecommerce.order.repositories.ReturnRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;

@Service
@RequiredArgsConstructor
public class ReturnService {

    private final OrderRepository orderRepository;
    private final ReturnRequestRepository returnRequestRepository;

    @Transactional
    public ReturnResponseDTO createReturn(ReturnRequestDTO request) {

        if (request.orderId() == null
                || request.orderItemId() == null
                || request.userId() == null
                || request.userId().isBlank()) {
            throw new IllegalArgumentException(
                    "Order ID, item ID and user ID are required"
            );
        }

        if (request.quantity() == null || request.quantity() <= 0) {
            throw new IllegalArgumentException(
                    "Return quantity must be greater than zero"
            );
        }

        if (request.reason() == null || request.reason().isBlank()) {
            throw new IllegalArgumentException(
                    "Return reason is required"
            );
        }

        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Order not found"));

        if (!order.getUserId().equals(request.userId())) {
            throw new IllegalArgumentException(
                    "This order does not belong to the specified user"
            );
        }

        if (order.getStatus() != Orderstatus.DELIVERED) {
            throw new IllegalArgumentException(
                    "Only delivered orders can be returned"
            );
        }

        OrderItem item = order.getItems().stream()
                .filter(orderItem ->
                        orderItem.getId().equals(request.orderItemId()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Item does not belong to this order"
                        ));

        if (request.quantity() > item.getQuantity()) {
            throw new IllegalArgumentException(
                    "Return quantity exceeds purchased quantity"
            );
        }

        boolean activeReturnExists =
                returnRequestRepository.existsByOrderItem_IdAndStatusIn(
                        item.getId(),
                        EnumSet.of(
                                ReturnStatus.REQUESTED,
                                ReturnStatus.APPROVED,
                                ReturnStatus.RECEIVED
                        )
                );

        if (activeReturnExists) {
            throw new IllegalArgumentException(
                    "An active return request already exists for this item"
            );
        }

        BigDecimal refundAmount = item.getPrice()
                .multiply(BigDecimal.valueOf(request.quantity()));

        ReturnRequest returnRequest = new ReturnRequest();
        returnRequest.setOrder(order);
        returnRequest.setOrderItem(item);
        returnRequest.setQuantity(request.quantity());
        returnRequest.setReason(request.reason().trim());
        returnRequest.setRequestedRefundAmount(refundAmount);
        returnRequest.setStatus(ReturnStatus.REQUESTED);

        ReturnRequest saved = returnRequestRepository.save(returnRequest);

        return new ReturnResponseDTO(
                saved.getId(),
                order.getId(),
                item.getId(),
                saved.getQuantity(),
                saved.getReason(),
                saved.getRequestedRefundAmount(),
                saved.getStatus(),
                saved.getCreatedAt()
        );
    }
}
