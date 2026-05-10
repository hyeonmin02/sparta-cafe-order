package org.example.spartacafe.global.config.kafka;

// Kafka 토픽 이름을 상수로 관리
// Producer와 Consumer 양쪽에서 이 클래스를 참조하므로
// 이름 오타나 불일치로 인한 연결 실패를 방지할 수 있음
public final class KafkaTopic {

    // 인스턴스 생성 방지 — 상수만 담는 클래스이므로 new KafkaTopic() 사용 불필요
    private KafkaTopic() {}

    public static final String ORDER_COMPLETED = "order.completed";
}
