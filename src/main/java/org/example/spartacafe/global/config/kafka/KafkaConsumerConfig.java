package org.example.spartacafe.global.config.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.example.spartacafe.domain.order.event.OrderCompletedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootStrapServers;

    // Consumer 공통 설정 — 컨슈머 그룹별로 재사용하기 위해 메서드로 분리
    private Map<String, Object> baseConsumerProps(String groupId) {
        Map<String, Object> props = new HashMap<>();

        // 연결할 Kafka 브로커 주소
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServers);

        // 컨슈머 그룹 ID — 같은 그룹 내 컨슈머들은 파티션을 나눠서 처리함 (중복 소비 방지)
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        // 메시지 Key는 String으로 역직렬화
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // 이 컨슈머 그룹이 처음 시작할 때 가장 최신 메시지부터 읽음
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        return props;
    }

    // Consumer가 Kafka 브로커에 연결하고 JSON 메시지를 OrderCompletedEvent로 역직렬화하는 방법을 정의
    @Bean
    public ConsumerFactory<String, OrderCompletedEvent> orderEventConsumerFactory() {
        JsonDeserializer<OrderCompletedEvent> deserializer = new JsonDeserializer<>(OrderCompletedEvent.class);

        // Producer에서 ADD_TYPE_INFO_HEADERS=false로 설정했으므로 타입 헤더가 없음 → 헤더 무시
        deserializer.setUseTypeHeaders(false);

        // 역직렬화 대상 패키지 신뢰 허용 — 단일 서비스이므로 전체 허용
        deserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(
                baseConsumerProps("popular-menu-group"),
                new StringDeserializer(),
                deserializer
        );
    }

    // @KafkaListener가 실제로 참조하는 컨테이너 팩토리
    // orderEventConsumerFactory의 설정을 기반으로 리스너를 실행할 컨테이너를 생성
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCompletedEvent> orderEventListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, OrderCompletedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderEventConsumerFactory());
        return factory;
    }
}
