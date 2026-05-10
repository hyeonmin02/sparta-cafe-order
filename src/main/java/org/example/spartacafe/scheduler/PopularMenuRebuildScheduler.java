package org.example.spartacafe.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.spartacafe.domain.order.repository.OrderQueryRepository;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class PopularMenuRebuildScheduler {

    private static final String POPULAR_MENU_KEY = "menu:popular";

    // 다중 인스턴스 환경에서 스케줄러 중복 실행을 막기 위한 분산락 키 (C6 시나리오)
    private static final String SCHEDULER_LOCK_KEY = "lock:scheduler:popular-menu-rebuild";

    private static final int REBUILD_DAYS = 7; // 최근 7일 주문 기준으로 집계

    private final OrderQueryRepository orderQueryRepository;
    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;

    // 매일 03:00 실행
    // Kafka 메시지 유실 등으로 Redis ZSET과 DB 간 불일치가 생긴 경우 DB 기준으로 보정 (C5 시나리오)
    @Scheduled(cron = "0 0 3 * * *")
    public void rebuild() {
        RLock lock = redissonClient.getLock(SCHEDULER_LOCK_KEY);
        boolean acquired = false;
        try {
            // waitTime=0 : 락을 즉시 획득 못 하면 기다리지 않고 바로 스킵
            // leaseTime=30s : 스케줄러가 예외로 멈춰도 30초 후 자동 해제
            acquired = lock.tryLock(0, 30, TimeUnit.SECONDS);
            if (!acquired) {
                // 다른 인스턴스가 이미 실행 중 → 중복 실행 없이 종료
                log.info("[Scheduler] 다른 인스턴스가 이미 보정 중 — 스킵");
                return;
            }
            rebuildPopularMenuZSet();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[Scheduler] 인기 메뉴 보정 중 인터럽트 발생", e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void rebuildPopularMenuZSet() {
        log.info("[Scheduler] 인기 메뉴 ZSET 보정 시작");

        // DB에서 최근 7일간 메뉴별 주문 수량 합계 조회 (내림차순)
        LocalDateTime from = LocalDateTime.now().minusDays(REBUILD_DAYS);
        List<Object[]> results = orderQueryRepository.sumQuantityGroupByMenuIdSince(from);

        // 기존 ZSET 전체 삭제 후 DB 집계 결과로 재구성
        // → Kafka 유실로 누락된 점수나 잘못 쌓인 점수가 모두 DB 기준으로 정정됨
        redisTemplate.delete(POPULAR_MENU_KEY);

        for (Object[] row : results) {
            Long menuId      = (Long) row[0];
            Long totalQty    = (Long) row[1];
            redisTemplate.opsForZSet().add(POPULAR_MENU_KEY, String.valueOf(menuId), totalQty);
        }

        log.info("[Scheduler] 인기 메뉴 ZSET 보정 완료 — {}개 메뉴 반영", results.size());
    }
}
