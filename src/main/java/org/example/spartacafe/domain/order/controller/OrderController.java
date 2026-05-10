package org.example.spartacafe.domain.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.spartacafe.domain.order.dto.request.OrderRequest;
import org.example.spartacafe.domain.order.dto.response.OrderResponse;
import org.example.spartacafe.domain.order.service.OrderService;
import org.example.spartacafe.global.response.ApiResponse;
import org.example.spartacafe.global.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "주문", description = "주문 생성 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "주문 생성", description = "재고 차감 및 포인트 차감 후 주문을 생성합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody OrderRequest request) {
        OrderResponse order = orderService.createOrder(userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(order));
    }
}
