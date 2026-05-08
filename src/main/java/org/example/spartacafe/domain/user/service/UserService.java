package org.example.spartacafe.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.example.spartacafe.domain.user.dto.request.LoginRequest;
import org.example.spartacafe.domain.user.dto.request.SignUpRequest;
import org.example.spartacafe.domain.user.dto.response.LoginResponse;
import org.example.spartacafe.domain.user.dto.response.SignUpResponse;
import org.example.spartacafe.domain.user.entity.User;
import org.example.spartacafe.domain.user.repository.UserRepository;
import org.example.spartacafe.global.exception.BusinessException;
import org.example.spartacafe.global.exception.ErrorCode;
import org.example.spartacafe.global.security.JwtProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public SignUpResponse signup(SignUpRequest request) {
        // 로그인 아이디 중복 체크
        if (userRepository.existsByLoginId(request.loginId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.create(request.loginId(), encodedPassword);

        userRepository.save(user);

        return new SignUpResponse(user.getId());
    }


    @Transactional
    public LoginResponse login(LoginRequest request) {
        // 아이디 존재 여부 확인
        User user = userRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 토큰 발급
        String accessToken = jwtProvider.createAccessToken(
                user.getId(), user.getLoginId(), user.getUserRole().name());

        // accessExpirationTime은 ms 단위 → 초 단위로 변환 (1800000ms / 1000 = 1800초 = 30분)
        return LoginResponse.of(accessToken, jwtProvider.getAccessExpirationTime() / 1000);
    }
}

