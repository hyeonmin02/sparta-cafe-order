package org.example.spartacafe.domain.menu.dto;

import org.example.spartacafe.domain.menu.entity.Menu;

public record MenuResponse(
        Long id,
        String name,
        Long price,
        String categoryName,
        boolean soldOut
) {
    public static MenuResponse from(Menu menu, boolean soldOut) {
        return new MenuResponse(
                menu.getId(),
                menu.getName(),
                menu.getPrice(),
                menu.getCategory().getCategoryName(),
                soldOut
        );
    }
}
