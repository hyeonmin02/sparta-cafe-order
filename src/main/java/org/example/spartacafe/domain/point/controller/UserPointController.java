package org.example.spartacafe.domain.point.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.spartacafe.domain.point.dto.request.PointChargeRequest;
import org.example.spartacafe.domain.point.dto.response.PointBalanceResponse;
import org.example.spartacafe.domain.point.dto.response.PointHistoryResponse;
import org.example.spartacafe.domain.point.service.UserPointService;
import org.example.spartacafe.global.response.ApiResponse;
import org.example.spartacafe.global.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "포인트", description = "포인트 충전 및 조회 API")
@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class UserPointController {

    private final UserPointService userPointService;

    @Operation(summary = "포인트 충전", description = "지정한 금액만큼 포인트를 충전합니다. 최소 100원, 최대 1,000,000원")
    @PostMapping("/charge")
    public ResponseEntity<ApiResponse<String>> chargePoint(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PointChargeRequest request) {

        userPointService.chargePoint(userDetails.getUserId(), request);

        String message = request.amount() + "원 충전이 완료되었습니다.";
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    @Operation(summary = "포인트 잔액 조회", description = "현재 보유 포인트 잔액을 조회합니다.")
    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<PointBalanceResponse>> getBalance(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        PointBalanceResponse balance = userPointService.getBalance(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(balance));
    }

    @Operation(summary = "포인트 내역 조회", description = "충전 및 사용 내역을 최신순으로 조회합니다.")
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<PointHistoryResponse>>> getHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<PointHistoryResponse> history = userPointService.getHistory(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
