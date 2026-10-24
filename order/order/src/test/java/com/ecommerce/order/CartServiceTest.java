
package com.ecommerce.order.services;

import com.ecommerce.order.dto.CartItemRequest;
import com.ecommerce.order.models.CartItem;
import com.ecommerce.order.repositories.CartItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @InjectMocks
    private CartService cartService;

    private CartItemRequest request;
    private CartItem existingItem;

    @BeforeEach
    void setUp() {
        request = new CartItemRequest();
        request.setProductId("101");
        request.setQuantity(2);

        existingItem = new CartItem();
        existingItem.setUserId(1L);
        existingItem.setProductId(101L);
        existingItem.setQuantity(3);
        existingItem.setPrice(BigDecimal.valueOf(1000.00));
    }

    @Test
    void addToCart_whenItemDoesNotExist_shouldCreateAndSaveItem() {
        when(cartItemRepository.findByUserIdAndProductId("1", "101"))
                .thenReturn(null);

        boolean result = cartService.addToCart("1", request);

        assertTrue(result);

        ArgumentCaptor<CartItem> captor =
                ArgumentCaptor.forClass(CartItem.class);

        verify(cartItemRepository).save(captor.capture());

        CartItem savedItem = captor.getValue();

        assertEquals(1L, savedItem.getUserId());
        assertEquals(101L, savedItem.getProductId());
        assertEquals(2, savedItem.getQuantity());
        assertEquals(
                0,
                BigDecimal.valueOf(1000.00).compareTo(savedItem.getPrice())
        );
    }

    @Test
    void addToCart_whenItemAlreadyExists_shouldIncreaseQuantity() {
        when(cartItemRepository.findByUserIdAndProductId("1", "101"))
                .thenReturn(existingItem);

        boolean result = cartService.addToCart("1", request);

        assertTrue(result);
        assertEquals(5, existingItem.getQuantity());
        assertEquals(
                0,
                BigDecimal.valueOf(1000.00)
                        .compareTo(existingItem.getPrice())
        );

        verify(cartItemRepository).save(existingItem);
    }

    @Test
    void deleteItemFromCart_whenItemExists_shouldDeleteAndReturnTrue() {
        when(cartItemRepository.findByUserIdAndProductId("1", "101"))
                .thenReturn(existingItem);

        boolean result =
                cartService.deleteItemFromCart("1", "101");

        assertTrue(result);

        verify(cartItemRepository)
                .delete(existingItem);
    }

    @Test
    void deleteItemFromCart_whenItemDoesNotExist_shouldReturnFalse() {
        when(cartItemRepository.findByUserIdAndProductId("1", "101"))
                .thenReturn(null);

        boolean result =
                cartService.deleteItemFromCart("1", "101");

        assertFalse(result);

        verify(cartItemRepository, never())
                .delete(any(CartItem.class));
    }

    @Test
    void getCart_shouldReturnItemsForUser() {
        List<CartItem> items = List.of(existingItem);

        when(cartItemRepository.findByUserId("1"))
                .thenReturn(items);

        List<CartItem> result = cartService.getCart("1");

        assertEquals(1, result.size());
        assertSame(existingItem, result.get(0));

        verify(cartItemRepository).findByUserId("1");
    }

    @Test
    void getCart_whenCartIsEmpty_shouldReturnEmptyList() {
        when(cartItemRepository.findByUserId("1"))
                .thenReturn(List.of());

        List<CartItem> result = cartService.getCart("1");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void clearCart_shouldDeleteAllItemsForUser() {
        cartService.clearCart("1");

        verify(cartItemRepository).deleteByUserId("1");
    }
}
