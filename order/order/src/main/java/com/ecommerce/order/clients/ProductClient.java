
package com.ecommerce.order.clients;

import com.ecommerce.order.dto.ProductResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ProductClient {

    private final RestClient restClient;

    public ProductClient(
            @Value("${product.service.base-url}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    public ProductResponseDTO getProductById(Long productId) {
        return restClient.get()
                .uri("/api/products/{id}", productId)
                .retrieve()
                .body(ProductResponseDTO.class);
    }
}
