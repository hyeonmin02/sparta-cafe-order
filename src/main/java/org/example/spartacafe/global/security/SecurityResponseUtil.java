package org.example.spartacafe.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.example.spartacafe.global.exception.ErrorCode;
import org.example.spartacafe.global.response.ErrorResponse;
import org.springframework.http.MediaType;

import java.io.IOException;

public class SecurityResponseUtil {
    private SecurityResponseUtil() {}  // 인스턴스화 방지

    public static void writeErrorResponse(
            HttpServletResponse response,
            ErrorCode errorCode,
            ObjectMapper objectMapper) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(ErrorResponse.of(errorCode))
        );
    }
}
