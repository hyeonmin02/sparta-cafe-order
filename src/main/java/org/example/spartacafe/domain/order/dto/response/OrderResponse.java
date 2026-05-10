package org.example.spartacafe.domain.order.dto.response;

import org.example.spartacafe.domain.order.entity.Order;
import org.example.spartacafe.domain.order.entity.OrderItem;
import org.example.spartacafe.domain.order.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long orderId,
        OrderStatus status,
        Long totalAmount,
        List<OrderItemResponse> items,
        LocalDateTime createdAt
) {
    public record OrderItemResponse(
            Long menuId,
            String menuName,
            Long unitPrice,
            int quantity,
            Long subTotal
    ) {
        public static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(
                    item.getMenuId(),
                    item.getMenuName(),
                    item.getUnitPrice(),
                    item.getQuantity(),
                    item.getSubTotal()
            );
        }
    }

    public static OrderResponse from(Order order, List<OrderItem> items) {
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getTotalAmount(),
                items.stream().map(OrderItemResponse::from).toList(),
                order.getCreatedAt()
        );
    }
}
