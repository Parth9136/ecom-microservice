
package com.ecommerce.order.services;

import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.models.CartItem;
import com.ecommerce.order.models.Order;
import com.ecommerce.order.models.OrderItem;
import com.ecommerce.order.models.Orderstatus;
import com.ecommerce.order.repositories.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private CartService cartService;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private CartItem firstItem;
    private CartItem secondItem;

    @BeforeEach
    void setUp() {
        firstItem = new CartItem();
        firstItem.setUserId(1L);
        firstItem.setProductId(101L);
        firstItem.setQuantity(2);
        firstItem.setPrice(new BigDecimal("1000.00"));

        secondItem = new CartItem();
        secondItem.setUserId(1L);
        secondItem.setProductId(202L);
        secondItem.setQuantity(3);
        secondItem.setPrice(new BigDecimal("500.00"));
    }

    @Test
    void createOrder_whenCartIsEmpty_shouldReturnEmpty() {
        when(cartService.getCart("1")).thenReturn(List.of());

        Optional<OrderResponse> result =
                orderService.createOrder("1");

        assertTrue(result.isEmpty());

        verify(cartService).getCart("1");
        verifyNoInteractions(orderRepository);
        verify(cartService, never()).clearCart("1");
    }

    @Test
    void createOrder_whenCartHasItems_shouldSaveOrderAndClearCart() {
        when(cartService.getCart("1"))
                .thenReturn(List.of(firstItem, secondItem));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    order.setId(10L);
                    return order;
                });

        Optional<OrderResponse> result =
                orderService.createOrder("1");

        assertTrue(result.isPresent());

        OrderResponse response = result.get();

        assertEquals(10L, response.getId());
        assertEquals(
                0,
                new BigDecimal("1500.00")
                        .compareTo(response.getTotalAmount())
        );
        assertEquals(Orderstatus.CONFIRMED, response.getStatus());
        assertEquals(2, response.getItems().size());

        verify(orderRepository).save(any(Order.class));
        verify(cartService).clearCart("1");
    }

    @Test
    void createOrder_shouldSaveCorrectOrderAndItems() {
        when(cartService.getCart("1"))
                .thenReturn(List.of(firstItem, secondItem));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    order.setId(10L);
                    return order;
                });

        orderService.createOrder("1");

        ArgumentCaptor<Order> captor =
                ArgumentCaptor.forClass(Order.class);

        verify(orderRepository).save(captor.capture());

        Order savedOrder = captor.getValue();

        assertEquals("1", savedOrder.getUserId());
        assertEquals(Orderstatus.CONFIRMED, savedOrder.getStatus());
        assertEquals(
                0,
                new BigDecimal("1500.00")
                        .compareTo(savedOrder.getTotalAmount())
        );

        List<OrderItem> savedItems = savedOrder.getItems();

        assertEquals(2, savedItems.size());

        assertEquals(101L, savedItems.get(0).getProductId());
        assertEquals(2, savedItems.get(0).getQuantity());

        assertEquals(202L, savedItems.get(1).getProductId());
        assertEquals(3, savedItems.get(1).getQuantity());
    }

    @Test
    void createOrder_shouldMapItemSubtotalUsingQuantity() {
        when(cartService.getCart("1"))
                .thenReturn(List.of(firstItem));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    order.setId(10L);
                    return order;
                });

        Optional<OrderResponse> result =
                orderService.createOrder("1");

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getItems().size());

        var responseItem = result.get().getItems().get(0);

        assertEquals(101L, responseItem.getProductId());
        assertEquals(2, responseItem.getQuantity());
        assertEquals(
                0,
                new BigDecimal("1000.00")
                        .compareTo(responseItem.getPrice())
        );
    }

    @Test
    void createOrder_shouldClearCartAfterSavingOrder() {
        when(cartService.getCart("1"))
                .thenReturn(List.of(firstItem));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        orderService.createOrder("1");

        var order = inOrder(orderRepository, cartService);

        order.verify(orderRepository).save(any(Order.class));
        order.verify(cartService).clearCart("1");
    }
}
