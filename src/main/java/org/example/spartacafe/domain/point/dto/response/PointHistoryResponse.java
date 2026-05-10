package org.example.spartacafe.domain.point.dto.response;

import org.example.spartacafe.domain.point.entity.PointHistory;
import org.example.spartacafe.domain.point.enums.PointType;

import java.time.LocalDateTime;

public record PointHistoryResponse(
        Long id,
        PointType type,
        Long amount,
        Long balanceAfter,
        Long relatedOrderId,
        LocalDateTime createdAt
) {
    public static PointHistoryResponse from(PointHistory history) {
        return new PointHistoryResponse(
                history.getId(),
                history.getType(),
                history.getAmount(),
                history.getBalanceAfter(),
                history.getRelatedOrderId(),
                history.getCreatedAt()
        );
    }
}
