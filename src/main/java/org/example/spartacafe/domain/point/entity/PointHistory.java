package org.example.spartacafe.domain.point.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.spartacafe.domain.point.enums.PointType;
import org.example.spartacafe.domain.user.entity.User;
import org.example.spartacafe.global.common.BaseEntity;

@Entity
@Table(name = "point_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointType type;

    @Column(nullable = false)
    private Long amount; // 변동 금액 (+1000 충전 -1000 사용)

    @Column(nullable = false)
    private Long balanceAfter; // 변동 후 남은 포인트 잔액

    @Column(nullable = false)
    private Long relatedOrderId; // 사용 시 주문 ID

    // 포인트 충전 기록 메서드
    public static PointHistory ofCharge(User user, Long amount, Long balanceAfter) {
        PointHistory history = new PointHistory();
        history.user = user;
        history.type = PointType.CHARGE;
        history.amount = amount;
        history.balanceAfter = balanceAfter;
        history.relatedOrderId = null;
        return history;
    }

    // 포인트 사용 기록 메서드
    public static PointHistory ofUse(User user, Long amount, Long balanceAfter, Long orderId) {
        PointHistory history = new PointHistory();
        history.user = user;
        history.type = PointType.USE;
        history.amount = -amount;    // 사용은 음수
        history.balanceAfter = balanceAfter;
        history.relatedOrderId = orderId;
        return history;
    }
}
