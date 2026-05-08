package org.example.spartacafe.global.security;

import org.example.spartacafe.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static org.example.spartacafe.global.security.SecurityResponseUtil.writeErrorResponse;

/**
 * [JWT 인증 필터] 모든 HTTP 요청에서 JWT를 검증하고 인증 정보를 SecurityContext에 저장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String jwt = jwtProvider.resolveToken(request);

        if (!StringUtils.hasText(jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            jwtProvider.validateTokenOrThrow(jwt);
            Authentication authentication = jwtProvider.getAuthentication(jwt);
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.warn("유효하지 않은 JWT 서명입니다.");
            writeErrorResponse(response, ErrorCode.TOKEN_INVALID,objectMapper);
            return;
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다.");
            writeErrorResponse(response, ErrorCode.TOKEN_EXPIRED, objectMapper);
            return;
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰입니다.");
            writeErrorResponse(response, ErrorCode.TOKEN_INVALID, objectMapper);
            return;
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims가 비어있습니다.");
            writeErrorResponse(response, ErrorCode.TOKEN_INVALID, objectMapper);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
