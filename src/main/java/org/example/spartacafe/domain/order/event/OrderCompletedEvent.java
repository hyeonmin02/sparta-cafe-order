package org.example.spartacafe.domain.order.event;

import java.util.List;

// 주문 완료 시 Kafka 토픽으로 발행하는 이벤트
// Producer가 이 객체를 JSON으로 변환해서 전송하고,
// Consumer가 JSON을 다시 이 객체로 변환해서 Redis 점수를 누적한다
public record OrderCompletedEvent(List<OrderItemInfo> items) {

    // 인기 메뉴 집계에 필요한 정보만 담음
    // menuId로 Redis ZSET 멤버를 식별하고, quantity만큼 점수를 더함
    public record OrderItemInfo(Long menuId, int quantity) {}
}
