package org.example.spartacafe.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * [JWT] 토큰 생성, 추출, 검증을 담당하는 컴포넌트
 * <p>
 * JJWT 0.12.6 버전을 사용하며, Access Token 단독 체제로 운영됩니다.
 * subject에는 userId(Long)를, 클레임에는 loginId와 role을 담습니다.
 *
 * <pre>
 * [application.yml 설정 예시]
 * jwt:
 *   secret-key: ${JWT_SECRET}  # 32바이트(256bit) 이상 필수 (HS256 요구사항)
 *   access-expiration-time: 1800000 (30분, 단위: ms)
 * </pre>
 */
@Slf4j
@Component
public class JwtProvider {

    private static final int MIN_SECRET_KEY_BYTES = 32;

    private final SecretKey key;

    @Getter// getAccessExpirationTime만 자동 생성되도록 필드레벨 선언
    private final long accessExpirationTime;

    public JwtProvider(
            @Value("${jwt.secret-key}") String secretKey,
            @Value("${jwt.access-expiration-time}") long accessExpirationTime) {
        // UTF-8 명시를 통해 환경에 독립적인 키 생성
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_KEY_BYTES) {
            throw new IllegalArgumentException(
                    "JWT secret key는 최소 " + MIN_SECRET_KEY_BYTES + "바이트(256bit) 이상이어야 합니다. "
                            + "현재: " + keyBytes.length + "바이트");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessExpirationTime = accessExpirationTime;
    }

    /**
     * Access Token 생성
     * @param userId 회원 고유 번호 (subject)
     * @param loginId 회원 로그인 아이디 (claim)
     * @param role 회원 권한 (claim) — "USER" 형태로 전달, ROLE_ 접두사는 UserDetails에서 처리
     * @return 생성된 JWT 토큰
     */
    public String createAccessToken(Long userId, String loginId, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessExpirationTime);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("loginId", loginId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    /**
     * 토큰에서 인증 정보 추출
     * @param token JWT 토큰
     * @return Security Authentication 객체
     */
    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);

        Long userId = Long.parseLong(claims.getSubject());
        String loginId = claims.get("loginId", String.class);
        String role = claims.get("role", String.class);

        CustomUserDetails userDetails = new CustomUserDetails(userId, loginId, role);
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    /**
     * 토큰의 남은 유효시간(ms) 반환
     * 블랙리스트 등록이나 TTL 설정 시 사용 (현재 본 시스템은 블랙리스트 미사용)
     */
    public long getRemainingExpiration(String token) {
        long expiry = parseClaims(token).getExpiration().getTime();
        long remaining = expiry - System.currentTimeMillis();
        return Math.max(remaining, 0);
    }

    /**
     * 토큰 유효성 검증 (예외 발생 시 상위 필터로 전파)
     * JwtAuthFilter에서 정확한 에러 응답을 위해 사용
     */
    public void validateTokenOrThrow(String token) {
        Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
    }

    /**
     * 단순 유효성 검증 (로그 기록용, 부울 반환)
     */
    public boolean validateToken(String token) {
        try {
            validateTokenOrThrow(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.warn("유효하지 않은 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims가 비어있습니다.");
        }
        return false;
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     */
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 토큰 파싱 및 클레임 추출
     * 만료된 토큰에서도 정보를 읽을 수 있도록 ExpiredJwtException을 캐치하여 클레임 반환
     */
    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}