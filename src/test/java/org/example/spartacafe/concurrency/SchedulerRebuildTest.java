package org.example.spartacafe.concurrency;

import org.example.spartacafe.IntegrationTestBase;
import org.example.spartacafe.scheduler.PopularMenuRebuildScheduler;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulerRebuildTest extends IntegrationTestBase {

    @Autowired
    private PopularMenuRebuildScheduler scheduler;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * C5: Kafka 메시지 유실로 Redis ZSET이 비어있을 때
     * 스케줄러가 DB 집계값으로 ZSET을 정확히 재구성해야 한다.
     */
    @Test
    void C5_Kafka유실_스케줄러보정_ZSET이_DB집계와_일치() {
        // given: DB에 주문/주문아이템 직접 삽입 (Kafka 미발행 상태 시뮬)
        jdbcTemplate.update(
                "INSERT INTO users (login_id, password, user_role) VALUES (?, ?, ?)",
                "user1", "pwd", "USER");
        Long userId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        // 주문 1 — menu_id=1(아메리카노) qty=3
        jdbcTemplate.update(
                "INSERT INTO orders (user_id, status, total_amount, created_at, updated_at) " +
                "VALUES (?, 'CREATED', 7500, NOW(), NOW())", userId);
        Long orderId1 = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbcTemplate.update(
                "INSERT INTO order_items (order_id, menu_id, menu_name, unit_price, quantity, sub_total) " +
                "VALUES (?, 1, '아메리카노', 2500, 3, 7500)", orderId1);

        // 주문 2 — menu_id=2(카페라떼) qty=2
        jdbcTemplate.update(
                "INSERT INTO orders (user_id, status, total_amount, created_at, updated_at) " +
                "VALUES (?, 'CREATED', 6000, NOW(), NOW())", userId);
        Long orderId2 = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbcTemplate.update(
                "INSERT INTO order_items (order_id, menu_id, menu_name, unit_price, quantity, sub_total) " +
                "VALUES (?, 2, '카페라떼', 3000, 2, 6000)", orderId2);

        // Redis ZSET은 비어있음 (Kafka 유실 시뮬)
        redisTemplate.delete("menu:popular");

        // when: 스케줄러 보정 실행
        scheduler.rebuild();

        // then: ZSET이 DB 집계와 일치
        Double scoreMenu1 = redisTemplate.opsForZSet().score("menu:popular", "1");
        Double scoreMenu2 = redisTemplate.opsForZSet().score("menu:popular", "2");
        assertThat(scoreMenu1).isEqualTo(3.0);
        assertThat(scoreMenu2).isEqualTo(2.0);
    }

    /**
     * C6: 다른 인스턴스가 이미 분산락을 보유한 상태에서 rebuild() 호출
     * → waitTime=0 이므로 락 획득 없이 즉시 스킵되어 ZSET이 변경되지 않아야 한다.
     */
    @Test
    void C6_분산락_선점시_스케줄러_중복실행_방지() throws InterruptedException {
        // given: Redis ZSET에 임의 점수 세팅
        redisTemplate.opsForZSet().add("menu:popular", "1", 99.0);

        // Redisson 락은 재진입(reentrant) 가능하므로 다른 스레드에서 점유해야 함
        CountDownLatch lockAcquired = new CountDownLatch(1);
        CountDownLatch testDone = new CountDownLatch(1);

        Thread lockHolder = new Thread(() -> {
            RLock lock = redissonClient.getLock("lock:scheduler:popular-menu-rebuild");
            lock.lock();
            lockAcquired.countDown();
            try {
                testDone.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        });
        lockHolder.start();
        lockAcquired.await(); // 락 점유 확인 후 진행

        try {
            // when: rebuild() 호출 — 다른 스레드가 락을 점유 중이므로 즉시 스킵
            scheduler.rebuild();

            // then: ZSET이 99.0 그대로 (rebuild 미실행)
            Double score = redisTemplate.opsForZSet().score("menu:popular", "1");
            assertThat(score).isEqualTo(99.0);
        } finally {
            testDone.countDown();
            lockHolder.join();
        }
    }
}
