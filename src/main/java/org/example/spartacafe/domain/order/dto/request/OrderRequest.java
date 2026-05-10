package org.example.spartacafe.domain.order.dto.request;

import java.util.List;

public record OrderRequest(
        List<OrderItemRequest> items
) {
    public record OrderItemRequest(
            Long menuId,
            int quantity
    ) {}
}
