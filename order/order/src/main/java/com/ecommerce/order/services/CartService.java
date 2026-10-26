
package com.ecommerce.order.services;

import com.ecommerce.order.clients.ProductClient;
import com.ecommerce.order.dto.CartItemRequest;
import com.ecommerce.order.dto.ProductResponseDTO;
import com.ecommerce.order.models.CartItem;
import com.ecommerce.order.repositories.CartItemRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductClient productClient;

    public boolean addToCart(String userId, CartItemRequest request) {

        // Fetch the latest product details from Product Service
        Long productId = Long.valueOf(request.getProductId());

        ProductResponseDTO product = productClient.getProductById(productId);

        if (product == null
                || product.price() == null
                || product.price().signum() < 0
                || !Boolean.TRUE.equals(product.active())) {
            throw new IllegalArgumentException(
                    "Product is unavailable or has an invalid price"
            );
        }

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero"
            );
        }

        CartItem existingCartItem =
                cartItemRepository.findByUserIdAndProductId(
                        userId, request.getProductId()
                );

        if (existingCartItem != null) {
            // Increase quantity and refresh the unit price
            existingCartItem.setQuantity(
                    existingCartItem.getQuantity() + request.getQuantity()
            );
            existingCartItem.setPrice(product.price());
            cartItemRepository.save(existingCartItem);

        } else {
            // Create a new cart item
            CartItem cartItem = new CartItem();
            cartItem.setUserId(Long.valueOf(userId));
            cartItem.setProductId(productId);
            cartItem.setQuantity(request.getQuantity());
            cartItem.setPrice(product.price());

            cartItemRepository.save(cartItem);
        }

        return true;
    }

    public boolean deleteItemFromCart(String userId, String productId) {
        CartItem cartItem =
                cartItemRepository.findByUserIdAndProductId(userId, productId);

        if (cartItem != null) {
            cartItemRepository.delete(cartItem);
            return true;
        }

        return false;
    }

    public List<CartItem> getCart(String userId) {
        return cartItemRepository.findByUserId(userId);
    }

    public void clearCart(String userId) {
        cartItemRepository.deleteByUserId(userId);
    }
}
