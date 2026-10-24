
package com.ecommerce.product;

import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.models.Product;
import com.ecommerce.product.repositories.ProductRepository;
import com.ecommerce.product.services.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(ProductCacheTest.TestConfig.class)
class ProductCacheTest {

    @Configuration
    @EnableCaching
    @Import(ProductService.class)
    static class TestConfig {

        @Bean
        ProductRepository productRepository() {
            return mock(ProductRepository.class);
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                    "products",
                    "productSearch"
            );
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    private ProductService productService;

    @org.springframework.beans.factory.annotation.Autowired
    private ProductRepository productRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private CacheManager cacheManager;

    private Product product;

    @BeforeEach
    void setUp() {
        cacheManager.getCache("products").clear();
        cacheManager.getCache("productSearch").clear();
        reset(productRepository);

        product = new Product();
        product.setId(1L);
        product.setName("Mechanical Keyboard");
        product.setDescription("RGB mechanical keyboard");
        product.setPrice(new BigDecimal("2499.00"));
        product.setStockQuantity(10);
        product.setCategory("Electronics");
        product.setImageUrl("keyboard.jpg");
        product.setActive(true);
    }

    @Test
    void getAllProducts_secondCallShouldUseCache() {
        when(productRepository.findByActiveTrue())
                .thenReturn(List.of(product));

        List<ProductResponse> first =
                productService.getAllProducts();

        List<ProductResponse> second =
                productService.getAllProducts();

        assertEquals(1, first.size());
        assertEquals(1, second.size());

        verify(productRepository, times(1)).findByActiveTrue();
    }

    @Test
    void searchProducts_secondCallShouldUseCache() {
        when(productRepository.searchProducts("keyboard"))
                .thenReturn(List.of(product));

        productService.searchProducts("keyboard");
        productService.searchProducts("keyboard");

        verify(productRepository, times(1))
                .searchProducts("keyboard");
    }

    @Test
    void updateProduct_shouldEvictProductCaches() {
        when(productRepository.findByActiveTrue())
                .thenReturn(List.of(product));

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        productService.getAllProducts();

        assertNotNull(
                cacheManager.getCache("products").get("allActive")
        );

        var request = new com.ecommerce.product.dto.ProductRequest();
        request.setName("Wireless Keyboard");
        request.setDescription("Updated keyboard");
        request.setPrice(new BigDecimal("1999.00"));
        request.setStockQuantity(8);
        request.setCategory("Electronics");
        request.setImageUrl("wireless-keyboard.jpg");

        productService.updateProduct(1L, request);

        assertNull(
                cacheManager.getCache("products").get("allActive")
        );
    }

    @Test
    void deleteProduct_shouldEvictProductCaches() {
        when(productRepository.findByActiveTrue())
                .thenReturn(List.of(product));

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        productService.getAllProducts();

        assertNotNull(
                cacheManager.getCache("products").get("allActive")
        );

        assertTrue(productService.deleteProduct(1L));

        assertNull(
                cacheManager.getCache("products").get("allActive")
        );
    }
}
