package org.example.spartacafe.domain.point.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.spartacafe.domain.point.enums.PointType;
import org.example.spartacafe.global.common.BaseEntity;

@Entity
@Table(name = "point_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PointType type;

    @Column(nullable = false)
    private Long amount; // 변동 금액 (+1000 충전 -1000 사용)

    @Column(nullable = false)
    private Long balanceAfter; // 변동 후 남은 포인트 잔액

    private Long relatedOrderId; // 사용 시 주문 ID, 충전 시 null

    // 포인트 충전 기록 메서드
    public static PointHistory ofCharge(Long userId, Long amount, Long balanceAfter) {
        PointHistory history = new PointHistory();
        history.userId = userId;
        history.type = PointType.CHARGE;
        history.amount = amount;
        history.balanceAfter = balanceAfter;
        history.relatedOrderId = null;
        return history;
    }

    // 포인트 사용 기록 메서드
    public static PointHistory ofUse(Long userId, Long amount, Long balanceAfter, Long orderId) {
        PointHistory history = new PointHistory();
        history.userId = userId;
        history.type = PointType.USE;
        history.amount = -amount;    // 사용은 음수
        history.balanceAfter = balanceAfter;
        history.relatedOrderId = orderId;
        return history;
    }
}
