package org.example.spartacafe.domain.order.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.spartacafe.domain.order.event.OrderCompletedEvent;
import org.example.spartacafe.global.config.kafka.KafkaTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderKafkaProducer {

    private final KafkaTemplate<String, OrderCompletedEvent> kafkaTemplate;

    // @TransactionalEventListener(AFTER_COMMIT)
    // OrderService에서 publishEvent()를 호출하면 Spring이 이 메서드를 등록해뒀다가
    // 트랜잭션이 성공적으로 커밋된 후에만 실행한다
    // → 주문이 DB에 확정된 이후에만 Kafka로 발행되므로 롤백 시 이벤트가 나가는 불일치 방지
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(OrderCompletedEvent event) {
        kafkaTemplate.send(KafkaTopic.ORDER_COMPLETED, event);
        log.info("[Kafka] 주문 완료 이벤트 발행 | items: {}", event.items());
    }
}
