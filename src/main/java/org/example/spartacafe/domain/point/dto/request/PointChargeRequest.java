package org.example.spartacafe.domain.point.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PointChargeRequest(
    @NotNull(message = "충전 금액을 입력해주세요.")
    @Min(value = 100, message = "충전 금액은 최소 100원 이상이어야 합니다.")
    @Max(value = 1_000_000, message = "충전 금액은 최대 1,000,000원 이하이어야 합니다.")
    Long amount
) {}
