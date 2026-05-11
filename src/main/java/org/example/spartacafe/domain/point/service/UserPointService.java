package org.example.spartacafe.domain.point.service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import org.example.spartacafe.domain.point.dto.request.PointChargeRequest;
import org.example.spartacafe.domain.point.dto.response.PointBalanceResponse;
import org.example.spartacafe.domain.point.dto.response.PointChargeResponse;
import org.example.spartacafe.domain.point.dto.response.PointHistoryResponse;
import org.example.spartacafe.domain.point.entity.PointHistory;
import org.example.spartacafe.domain.point.entity.UserPoint;
import org.example.spartacafe.domain.point.repository.PointHistoryRepository;
import org.example.spartacafe.domain.point.repository.UserPointRepository;
import org.example.spartacafe.global.exception.BusinessException;
import org.example.spartacafe.global.exception.ErrorCode;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserPointService {

    private static final String POINT_LOCK_PREFIX = "lock:point:";
    private static final long LOCK_WAIT_TIME = 3L;
    private static final long LOCK_LEASE_TIME = 5L;

    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final RedissonClient redissonClient;

    @Transactional
    public PointChargeResponse chargePoint(Long userId, PointChargeRequest request) {
        RLock lock = redissonClient.getLock(POINT_LOCK_PREFIX + userId);
        boolean acquired;
        try {
            acquired = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);

            // 락 대기중 스레드 강제 종료 발생 시 에러 반환
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.LOCK_INTERRUPTED);
        }

        // 3초 기다렸는데도 락 획득 실패 시 에러 반환
        if (!acquired) {
            throw new BusinessException(ErrorCode.LOCK_TIMEOUT);
        }
        try {
            // 유저 포인트 조회 (비관적 락으로 동시성 제어)
            UserPoint userPoint = userPointRepository.findByUserIdWithLock(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

            // 포인트 충전
            userPoint.charge(request.amount());

            // 충전 내역 저장
            PointHistory history = PointHistory.ofCharge(userId, request.amount(), userPoint.getBalance());
            pointHistoryRepository.save(history);
            return new PointChargeResponse(history.getBalanceAfter(),request.amount(),history.getCreatedAt());
        } finally {
            lock.unlock(); // 성공이든 실패든 반드시 락 해제
        }

    }

    // 포인트 잔액 조회
    public PointBalanceResponse getBalance(Long userId) {
        UserPoint userPoint = userPointRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return new PointBalanceResponse(userId, userPoint.getBalance());
    }

    // 포인트 내역 조회
    public List<PointHistoryResponse> getHistory(Long userId) {
        return pointHistoryRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(PointHistoryResponse::from)
                .toList();
    }
}
