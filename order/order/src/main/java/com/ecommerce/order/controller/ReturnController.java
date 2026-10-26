
package com.ecommerce.order.controller;

import com.ecommerce.order.dto.ReturnRequestDTO;
import com.ecommerce.order.dto.ReturnResponseDTO;
import com.ecommerce.order.services.ReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReturnResponseDTO createReturn(
            @RequestBody ReturnRequestDTO request) {
        return returnService.createReturn(request);
    }
}
