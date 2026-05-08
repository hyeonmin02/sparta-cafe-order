package org.example.spartacafe.domain.user.dto.response;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Long expiresIn // 토큰만료 남은 시간
) {
    public static LoginResponse of(String accessToken, Long expiresIn) {
        return new LoginResponse(accessToken, "Bearer", expiresIn);
    }
}
