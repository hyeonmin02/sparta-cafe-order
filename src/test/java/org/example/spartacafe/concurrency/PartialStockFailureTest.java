package org.example.spartacafe.concurrency;

import org.example.spartacafe.IntegrationTestBase;
import org.example.spartacafe.domain.order.dto.request.OrderRequest;
import org.example.spartacafe.domain.order.service.OrderService;
import org.example.spartacafe.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartialStockFailureTest extends IntegrationTestBase {

    @Autowired
    private OrderService orderService;

    /**
     * C4: 다중 메뉴 주문 중 한 메뉴의 재고가 부족하면 전체 트랜잭션이 롤백되어야 한다.
     * → 먼저 처리된 메뉴의 재고 차감도 원복되고, 주문/주문아이템 행도 생성되지 않아야 한다.
     */
    @Test
    void C4_다중메뉴_일부재고부족_전체롤백() {
        // given: menu_id=1 재고 5개, menu_id=2 재고 0개 (menuId 오름차순 락 획득 — 1 먼저 차감 후 2에서 실패)
        jdbcTemplate.update("UPDATE stocks SET quantity = 5 WHERE menu_id = 1");
        jdbcTemplate.update("UPDATE stocks SET quantity = 0 WHERE menu_id = 2");

        jdbcTemplate.update(
                "INSERT INTO users (login_id, password, user_role) VALUES (?, ?, ?)",
                "user1", "pwd", "USER");
        Long userId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbcTemplate.update(
                "INSERT INTO user_point (user_id, balance) VALUES (?, ?)",
                userId, 100_000L);

        // menu_id=1(qty=1) + menu_id=2(qty=1) → menu_id=2 재고 부족으로 예외 발생
        OrderRequest request = new OrderRequest(List.of(
                new OrderRequest.OrderItemRequest(1L, 1),
                new OrderRequest.OrderItemRequest(2L, 1)
        ));

        // when & then: 예외 발생
        assertThatThrownBy(() -> orderService.createOrder(userId, request))
                .isInstanceOf(BusinessException.class);

        // then: menu_id=1 재고 원상 복구 (5 그대로)
        Integer stock1 = jdbcTemplate.queryForObject(
                "SELECT quantity FROM stocks WHERE menu_id = 1", Integer.class);
        assertThat(stock1).isEqualTo(5);

        // menu_id=2 재고 변동 없음 (0 그대로)
        Integer stock2 = jdbcTemplate.queryForObject(
                "SELECT quantity FROM stocks WHERE menu_id = 2", Integer.class);
        assertThat(stock2).isZero();

        // 주문 미생성
        Integer orderCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE user_id = ?", Integer.class, userId);
        assertThat(orderCount).isZero();

        // 포인트 차감 없음 (잔액 100,000원 그대로)
        Long balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM user_point WHERE user_id = ?", Long.class, userId);
        assertThat(balance).isEqualTo(100_000L);
    }
}
