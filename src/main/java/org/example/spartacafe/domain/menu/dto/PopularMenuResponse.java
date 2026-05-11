package org.example.spartacafe.domain.menu.dto;

import org.example.spartacafe.domain.menu.entity.Menu;

import java.time.LocalDateTime;

public record PopularMenuResponse(
        int rank,
        Long menuId,
        String name,
        Long price,
        int orderCount,
        int windowDays,
        LocalDateTime calculatedAt
) {
    public static PopularMenuResponse of(int rank, Menu menu, int orderCount) {
        return new PopularMenuResponse(
                rank,
                menu.getId(),
                menu.getName(),
                menu.getPrice(),
                orderCount,
                7,
                LocalDateTime.now()
        );
    }
}
