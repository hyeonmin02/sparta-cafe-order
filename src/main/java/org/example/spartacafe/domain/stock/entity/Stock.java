package org.example.spartacafe.domain.stock.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.spartacafe.global.exception.BusinessException;
import org.example.spartacafe.global.exception.ErrorCode;

import java.util.Map;

@Entity
@Table(name = "stocks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long menuId;

    @Column(nullable = false)
    private int quantity;

    // 재고 차감
    public void decrease(int amount) {
        if(this.quantity < amount) {
            throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT);
        }
        this.quantity -= amount;
    }

    // 재고 증가
    public void increase(int amount) {
        this.quantity += amount;
    }
}
