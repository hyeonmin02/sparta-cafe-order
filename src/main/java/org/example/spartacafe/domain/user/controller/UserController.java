package org.example.spartacafe.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.spartacafe.domain.user.dto.request.LoginRequest;
import org.example.spartacafe.domain.user.dto.request.SignUpRequest;
import org.example.spartacafe.domain.user.dto.response.LoginResponse;
import org.example.spartacafe.domain.user.dto.response.SignUpResponse;
import org.example.spartacafe.domain.user.service.UserService;
import org.example.spartacafe.global.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증", description = "회원가입 및 로그인 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class UserController {

    private final UserService userService;

    @Operation(summary = "회원가입", description = "아이디와 비밀번호로 회원가입합니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignUpResponse>> signUp(
            @Valid @RequestBody SignUpRequest request) {
        SignUpResponse response = userService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "로그인", description = "로그인 성공 시 JWT Access Token을 반환합니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
