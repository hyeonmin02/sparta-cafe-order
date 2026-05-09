package org.example.spartacafe.domain.point.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.spartacafe.domain.point.dto.request.PointChargeRequest;
import org.example.spartacafe.domain.point.service.UserPointService;
import org.example.spartacafe.global.response.ApiResponse;
import org.example.spartacafe.global.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class UserPointController {

    private final UserPointService userPointService;

    @PostMapping("/charge")
    public ResponseEntity<ApiResponse<String>> chargePoint(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PointChargeRequest request) {

        userPointService.chargePoint(userDetails.getUserId(), request);

        String message = request.amount() + "원 충전이 완료되었습니다.";
        return ResponseEntity.ok(ApiResponse.success(message));
    }
}
