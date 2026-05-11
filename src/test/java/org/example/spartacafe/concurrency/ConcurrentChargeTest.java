package org.example.spartacafe.concurrency;

import org.example.spartacafe.IntegrationTestBase;
import org.example.spartacafe.domain.point.dto.request.PointChargeRequest;
import org.example.spartacafe.domain.point.service.UserPointService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ConcurrentChargeTest extends IntegrationTestBase {

    @Autowired
    private UserPointService userPointService;

    /**
     * C3: 동일 사용자가 1,000원씩 동시에 10번 충전
     * → Redisson 분산락 + 비관적 락으로 누락 없이 정확히 10,000원이 누적되어야 한다.
     */
    @Test
    void C3_동일사용자_동시충전_정확한_누적() throws InterruptedException {
        // given: 잔액 0원 유저
        jdbcTemplate.update(
                "INSERT INTO users (login_id, password, user_role) VALUES (?, ?, ?)",
                "user1", "pwd", "USER");
        Long userId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbcTemplate.update(
                "INSERT INTO user_point (user_id, balance) VALUES (?, ?)",
                userId, 0L);

        PointChargeRequest request = new PointChargeRequest(1_000L);

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    userPointService.chargePoint(userId, request);
                    success.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        done.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // then: 10건 모두 성공
        assertThat(success.get()).isEqualTo(10);

        // 잔액 정확히 10,000원
        Long balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM user_point WHERE user_id = ?", Long.class, userId);
        assertThat(balance).isEqualTo(10_000L);

        // 충전 내역 정확히 10건
        Integer historyCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM point_histories WHERE user_id = ?", Integer.class, userId);
        assertThat(historyCount).isEqualTo(10);
    }
}
