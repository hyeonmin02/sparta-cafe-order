package org.example.spartacafe.domain.menu.dto;

import org.example.spartacafe.domain.menu.entity.Menu;

public record PopularMenuResponse(
        int rank,        // 순위 (1위, 2위, 3위)
        Long menuId,
        String name,
        Long price,
        int orderCount   // Redis ZSET 누적 점수 — 주문된 총 수량 합계
) {
    public static PopularMenuResponse of(int rank, Menu menu, int orderCount) {
        return new PopularMenuResponse(rank, menu.getId(), menu.getName(), menu.getPrice(), orderCount);
    }
}
