package org.example.spartacafe.domain.order.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.spartacafe.domain.order.event.OrderCompletedEvent;
import org.example.spartacafe.global.config.kafka.KafkaTopic;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderKafkaConsumer {

    // Redis ZSET의 키 — 모든 메뉴의 인기 점수가 이 키 하나에 모임
    // 구조: menu:popular → { "1": 150.0, "3": 120.0, "5": 90.0 }
    //                          menuId   점수       menuId  점수
    private static final String POPULAR_MENU_KEY = "menu:popular";

    // StringRedisTemplate — Redis 명령어를 Java에서 호출할 수 있게 해주는 객체
    // opsForZSet()으로 ZSET 관련 명령어(ZINCRBY, ZREVRANGE 등)를 사용
    private final StringRedisTemplate redisTemplate;

    // @KafkaListener — 지정한 토픽을 구독하다가 메시지가 오면 이 메서드를 실행
    // containerFactory: KafkaConsumerConfig에서 등록한 팩토리 이름을 지정해야
    //                   JSON → OrderCompletedEvent 역직렬화가 올바르게 동작함
    @KafkaListener(
            topics = KafkaTopic.ORDER_COMPLETED,
            containerFactory = "orderEventListenerContainerFactory"
    )
    public void consume(OrderCompletedEvent event) {
        for (OrderCompletedEvent.OrderItemInfo item : event.items()) {
            // ZINCRBY menu:popular {quantity} {menuId}
            // 해당 menuId의 점수를 주문 수량만큼 증가시킴
            // 처음 등장하는 menuId라면 점수 0에서 시작
            redisTemplate.opsForZSet().incrementScore(
                    POPULAR_MENU_KEY,
                    String.valueOf(item.menuId()),
                    item.quantity()
            );
        }
        log.info("[Kafka] 인기 메뉴 점수 업데이트 | items: {}", event.items());
    }
}
