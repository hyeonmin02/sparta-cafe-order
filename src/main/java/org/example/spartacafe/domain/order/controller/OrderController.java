package org.example.spartacafe.domain.order.controller;

import lombok.RequiredArgsConstructor;
import org.example.spartacafe.domain.order.dto.request.OrderRequest;
import org.example.spartacafe.domain.order.dto.response.OrderResponse;
import org.example.spartacafe.domain.order.service.OrderService;
import org.example.spartacafe.global.response.ApiResponse;
import org.example.spartacafe.global.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody OrderRequest request) {
        OrderResponse order = orderService.createOrder(userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(order));
    }
}
