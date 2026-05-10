package org.example.spartacafe.global.config.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.example.spartacafe.domain.order.event.OrderCompletedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootStrapServers;

    // Producer가 Kafka 브로커에 연결하고 메시지를 전송할 때 필요한 설정 모음
    @Bean
    public ProducerFactory<String, OrderCompletedEvent> producerFactory() {
        Map<String, Object> config = new HashMap<>();

        // 연결할 Kafka 브로커 주소
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServers);

        // Key는 String으로 직렬화 (토픽 내 파티션 분배에 사용)
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // Value(이벤트 객체)는 JSON으로 직렬화해서 전송
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // 타입 정보를 헤더에 포함하지 않음 — Consumer 쪽에서 직접 타입을 지정하므로 불필요
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaProducerFactory<>(config);
    }

    // KafkaTemplate — 실제로 메시지를 토픽에 발행할 때 사용하는 객체
    // OrderKafkaProducer에서 kafkaTemplate.send(KafkaTopic.ORDER_COMPLETED, event) 형태로 호출
    @Bean
    public KafkaTemplate<String, OrderCompletedEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
