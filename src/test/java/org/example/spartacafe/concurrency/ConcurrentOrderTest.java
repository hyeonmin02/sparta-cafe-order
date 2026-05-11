package org.example.spartacafe.concurrency;

import org.example.spartacafe.IntegrationTestBase;
import org.example.spartacafe.domain.order.dto.request.OrderRequest;
import org.example.spartacafe.domain.order.service.OrderService;
import org.example.spartacafe.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ConcurrentOrderTest extends IntegrationTestBase {

    @Autowired
    private OrderService orderService;

    /**
     * C1: 동일 사용자가 잔액과 동일한 금액을 동시에 10번 주문
     * → 비관적 락으로 정확히 1건만 성공하고 잔액은 0이 되어야 한다.
     */
    @Test
    void C1_동일사용자_동시주문_잔액부족_1건만_성공() throws InterruptedException {
        // given: 잔액 2500원, 아메리카노(menu_id=1) 가격 2500원 → 1번만 결제 가능
        jdbcTemplate.update(
                "INSERT INTO users (login_id, password, user_role) VALUES (?, ?, ?)",
                "user1", "pwd", "USER");
        Long userId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbcTemplate.update(
                "INSERT INTO user_point (user_id, balance) VALUES (?, ?)",
                userId, 2500L);

        OrderRequest request = new OrderRequest(List.of(new OrderRequest.OrderItemRequest(1L, 1)));

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    orderService.createOrder(userId, request);
                    success.incrementAndGet();
                } catch (BusinessException e) {
                    fail.incrementAndGet();
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

        // then
        assertThat(success.get()).isEqualTo(1);
        assertThat(fail.get()).isEqualTo(9);

        Long balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM user_point WHERE user_id = ?", Long.class, userId);
        assertThat(balance).isZero();
    }

    /**
     * C2: 재고가 1개인 메뉴를 10명이 동시 주문
     * → 비관적 락으로 정확히 1명만 성공하고 재고는 0이 되어야 한다.
     */
    @Test
    void C2_재고1개_동시주문_10명_1명만_성공() throws InterruptedException {
        // given: 아메리카노(menu_id=1) 재고 1개, 유저 10명 각 잔액 100,000원
        jdbcTemplate.update("UPDATE stocks SET quantity = 1 WHERE menu_id = 1");

        List<Long> userIds = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            jdbcTemplate.update(
                    "INSERT INTO users (login_id, password, user_role) VALUES (?, ?, ?)",
                    "user" + i, "pwd", "USER");
            Long userId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            jdbcTemplate.update(
                    "INSERT INTO user_point (user_id, balance) VALUES (?, ?)",
                    userId, 100_000L);
            userIds.add(userId);
        }

        OrderRequest request = new OrderRequest(List.of(new OrderRequest.OrderItemRequest(1L, 1)));

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            final Long userId = userIds.get(i);
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    orderService.createOrder(userId, request);
                    success.incrementAndGet();
                } catch (BusinessException e) {
                    fail.incrementAndGet();
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

        // then
        assertThat(success.get()).isEqualTo(1);
        assertThat(fail.get()).isEqualTo(9);

        Integer stock = jdbcTemplate.queryForObject(
                "SELECT quantity FROM stocks WHERE menu_id = 1", Integer.class);
        assertThat(stock).isZero();
    }
}
