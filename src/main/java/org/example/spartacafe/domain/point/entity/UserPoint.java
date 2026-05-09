package org.example.spartacafe.domain.point.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.spartacafe.global.common.BaseEntity;
import org.example.spartacafe.global.exception.BusinessException;
import org.example.spartacafe.global.exception.ErrorCode;

import java.util.Map;

@Entity
@Table(name = "user_point")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPoint extends BaseEntity {
    @Id
    private Long userId;

    @Column(nullable = false)
    private Long balance; // 현재 잔액

    @Version
    private Long version;

    // 회원가입 시 초기 생성
    public static UserPoint create(Long userId) {
        UserPoint userPoint = new UserPoint();
        userPoint.userId = userId;
        userPoint.balance = 0L;
        return userPoint;
    }

    public void charge(Long amount) {
        this.balance += amount;
    }

    public void use(Long amount) {
        if (this.balance < amount) {
            throw new BusinessException(ErrorCode.POINT_INSUFFICIENT);
        }
        this.balance -= amount;
    }
}
