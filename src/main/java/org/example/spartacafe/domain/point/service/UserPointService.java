package org.example.spartacafe.domain.point.service;

import org.example.spartacafe.domain.point.dto.request.PointChargeRequest;
import org.example.spartacafe.domain.point.entity.PointHistory;
import org.example.spartacafe.domain.point.entity.UserPoint;
import org.example.spartacafe.domain.point.repository.PointHistoryRepository;
import org.example.spartacafe.domain.point.repository.UserPointRepository;
import org.example.spartacafe.domain.user.entity.User;
import org.example.spartacafe.domain.user.repository.UserRepository;
import org.example.spartacafe.global.exception.BusinessException;
import org.example.spartacafe.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserPointService {
    
    private final UserPointRepository userPointRepository;
    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;

    /**
     * 포인트 충전
     * @param userId 유저 식별값
     * @param request 충전 금액 DTO
     */
    @Transactional
    public void chargePoint(Long userId, PointChargeRequest request) {
        // 1. 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 2. 유저 포인트 조회 (비관적 락으로 동시성 제어)
        UserPoint userPoint = userPointRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        // 3. 포인트 충전 (엔티티 내에서 balance 증가)
        userPoint.charge(request.amount());

        // 4. 포인트 충전 내역 생성 및 저장
        PointHistory history = PointHistory.ofCharge(user, request.amount(), userPoint.getBalance());
        pointHistoryRepository.save(history);
    }
}
