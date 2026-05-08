package org.example.spartacafe.global.security;

import org.example.spartacafe.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static org.example.spartacafe.global.security.SecurityResponseUtil.writeErrorResponse;

/**
 * [EntryPoint] 인증되지 않은 사용자가 보호된 리소스에 접근했을 때 호출 (401 Unauthorized)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        log.warn("인증 실패: {}", authException.getMessage());
        writeErrorResponse(response, ErrorCode.UNAUTHORIZED, objectMapper);
    }
}
