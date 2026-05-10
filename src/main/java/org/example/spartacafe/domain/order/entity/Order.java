package org.example.spartacafe.domain.order.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.spartacafe.domain.order.enums.OrderStatus;
import org.example.spartacafe.global.common.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false)
    private Long totalAmount;

    private LocalDateTime paidAt;

    public static Order create(Long userId, Long totalAmount) {
        Order order = new Order();
        order.userId = userId;
        order.status = OrderStatus.CREATED;
        order.totalAmount = totalAmount;
        return order;
    }
}
