
package com.ecommerce.product;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.models.Product;
import com.ecommerce.product.repositories.ProductRepository;
import com.ecommerce.product.services.ProductService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private ProductRequest request;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setName("Mechanical Keyboard");
        product.setDescription("RGB mechanical keyboard");
        product.setPrice(new BigDecimal("2499.00"));
        product.setStockQuantity(10);
        product.setCategory("Electronics");
        product.setImageUrl("keyboard.jpg");
        product.setActive(true);

        request = new ProductRequest();
        request.setName("Mechanical Keyboard");
        request.setDescription("RGB mechanical keyboard");
        request.setPrice(new BigDecimal("2499.00"));
        request.setStockQuantity(10);
        request.setCategory("Electronics");
        request.setImageUrl("keyboard.jpg");
    }

    @Test
    void createProduct_shouldSaveAndReturnProduct() {
        when(productRepository.save(any(Product.class)))
                .thenReturn(product);

        ProductResponse response =
                productService.createProduct(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Mechanical Keyboard", response.getName());
        assertEquals(
                new BigDecimal("2499.00"),
                response.getPrice()
        );
        assertEquals(10, response.getStockQuantity());
        assertTrue(response.getActive());

        verify(productRepository, times(1))
                .save(any(Product.class));
    }

    @Test
    void getAllProducts_shouldReturnActiveProducts() {
        when(productRepository.findByActiveTrue())
                .thenReturn(List.of(product));

        List<ProductResponse> responses =
                productService.getAllProducts();

        assertEquals(1, responses.size());
        assertEquals(
                "Mechanical Keyboard",
                responses.get(0).getName()
        );

        verify(productRepository).findByActiveTrue();
    }

    @Test
    void updateProduct_whenProductExists_shouldUpdateProduct() {
        request.setName("Wireless Keyboard");
        request.setPrice(new BigDecimal("1999.00"));

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Optional<ProductResponse> result =
                productService.updateProduct(1L, request);

        assertTrue(result.isPresent());
        assertEquals(
                "Wireless Keyboard",
                result.get().getName()
        );
        assertEquals(
                new BigDecimal("1999.00"),
                result.get().getPrice()
        );

        verify(productRepository).findById(1L);
        verify(productRepository).save(product);
    }

    @Test
    void updateProduct_whenProductDoesNotExist_shouldReturnEmpty() {
        when(productRepository.findById(99L))
                .thenReturn(Optional.empty());

        Optional<ProductResponse> result =
                productService.updateProduct(99L, request);

        assertTrue(result.isEmpty());

        verify(productRepository, never())
                .save(any(Product.class));
    }

    @Test
    void deleteProduct_whenProductExists_shouldDeactivateProduct() {
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(productRepository.save(any(Product.class)))
                .thenReturn(product);

        boolean result = productService.deleteProduct(1L);

        assertTrue(result);
        assertFalse(product.getActive());

        verify(productRepository).save(product);
    }

    @Test
    void deleteProduct_whenProductDoesNotExist_shouldReturnFalse() {
        when(productRepository.findById(99L))
                .thenReturn(Optional.empty());

        boolean result = productService.deleteProduct(99L);

        assertFalse(result);

        verify(productRepository, never())
                .save(any(Product.class));
    }

    @Test
    void searchProducts_shouldReturnMatchingProducts() {
        when(productRepository.searchProducts("keyboard"))
                .thenReturn(List.of(product));

        List<ProductResponse> responses =
                productService.searchProducts("keyboard");

        assertEquals(1, responses.size());
        assertEquals(
                "Mechanical Keyboard",
                responses.get(0).getName()
        );

        verify(productRepository).searchProducts("keyboard");
    }

    @Test
    void searchProducts_whenNoMatches_shouldReturnEmptyList() {
        when(productRepository.searchProducts("unknown"))
                .thenReturn(List.of());

        List<ProductResponse> responses =
                productService.searchProducts("unknown");

        assertTrue(responses.isEmpty());
    }
}
