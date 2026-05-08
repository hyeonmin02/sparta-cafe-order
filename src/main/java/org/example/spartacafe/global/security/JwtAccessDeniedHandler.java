package org.example.spartacafe.global.security;

import org.example.spartacafe.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static org.example.spartacafe.global.security.SecurityResponseUtil.writeErrorResponse;

/**
 * [AccessDeniedHandler] 인증은 되었으나 해당 리소스에 대한 접근 권한이 없을 때 호출 (403 Forbidden)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        log.warn("권한 없음: {}", accessDeniedException.getMessage());
        writeErrorResponse(response, ErrorCode.FORBIDDEN, objectMapper);
    }
}
