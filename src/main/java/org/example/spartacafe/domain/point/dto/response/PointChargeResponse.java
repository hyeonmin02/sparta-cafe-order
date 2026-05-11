package org.example.spartacafe.domain.point.dto.response;

import java.time.LocalDateTime;

public record PointChargeResponse(
        long balance,
        long chargedAmount,
        LocalDateTime chargedAt
) {}
