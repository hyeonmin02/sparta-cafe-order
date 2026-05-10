package org.example.spartacafe.domain.order.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private Long menuId; // 참조용

    @Column(nullable = false, length = 100)
    private String menuName; // 주문 당시 이름 저장

    @Column(nullable = false)
    private Long unitPrice; // 주문 당시 가격 저장

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private Long subTotal; // 갯수 X 가격

    public static OrderItem create(Order order, Long menuId, String menuName, Long unitPrice, int quantity) {
        OrderItem item = new OrderItem();
        item.order = order;
        item.menuId = menuId;
        item.menuName = menuName;
        item.unitPrice = unitPrice;
        item.quantity = quantity;
        item.subTotal = unitPrice * quantity;
        return item;
    }
}
