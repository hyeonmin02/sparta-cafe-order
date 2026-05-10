package org.example.spartacafe.domain.order.service;

import lombok.RequiredArgsConstructor;
import org.example.spartacafe.domain.menu.entity.Menu;
import org.example.spartacafe.domain.menu.enums.MenuStatus;
import org.example.spartacafe.domain.menu.repository.MenuRepository;
import org.example.spartacafe.domain.order.dto.request.OrderRequest;
import org.example.spartacafe.domain.order.dto.response.OrderResponse;
import org.example.spartacafe.domain.order.entity.Order;
import org.example.spartacafe.domain.order.entity.OrderItem;
import org.example.spartacafe.domain.order.repository.OrderItemRepository;
import org.example.spartacafe.domain.order.repository.OrderRepository;
import org.example.spartacafe.domain.point.entity.PointHistory;
import org.example.spartacafe.domain.point.entity.UserPoint;
import org.example.spartacafe.domain.point.repository.PointHistoryRepository;
import org.example.spartacafe.domain.point.repository.UserPointRepository;
import org.example.spartacafe.domain.stock.entity.Stock;
import org.example.spartacafe.domain.stock.repository.StockRepository;
import org.example.spartacafe.global.exception.BusinessException;
import org.example.spartacafe.global.exception.ErrorCode;
import org.example.spartacafe.domain.order.event.OrderCompletedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuRepository menuRepository;
    private final StockRepository stockRepository;
    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public OrderResponse createOrder(Long userId, OrderRequest request) {

        // 1. 빈 주문 검증
        if (request.items() == null || request.items().isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_ITEMS_EMPTY);
        }

        // 2. menuId 오름차순 정렬 — 재고 락을 항상 같은 순서로 잡아 데드락 방지
        List<OrderRequest.OrderItemRequest> sortedItems = request.items().stream()
                .sorted(Comparator.comparingLong(OrderRequest.OrderItemRequest::menuId))
                .toList();

        List<Long> sortedMenuIds = sortedItems.stream()
                .map(OrderRequest.OrderItemRequest::menuId)
                .toList();

        // 3. 메뉴 일괄 조회 — findAllById 한 번으로 N개를 가져옴
        List<Menu> menus = menuRepository.findAllById(sortedMenuIds);
        if (menus.size() != sortedMenuIds.size()) {
            // 요청한 menuId 중 DB에 없는 것이 있음
            throw new BusinessException(ErrorCode.MENU_NOT_FOUND);
        }

        // menuId → Menu 로 빠르게 꺼내 쓰기 위한 Map
        Map<Long, Menu> menuMap = menus.stream()
                .collect(Collectors.toMap(Menu::getId, m -> m));

        // 4. 판매 중인 메뉴인지 검사
        for (Menu menu : menus) {
            if (menu.getStatus() != MenuStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.MENU_NOT_AVAILABLE);
            }
        }

        // 5. 재고 비관적 락 획득 — sortedMenuIds 순서 그대로 DB 락을 잡음 (데드락 방지 핵심)
        //    stream이 순서를 보장하므로 menuId 오름차순으로 락이 획득됨
        List<Stock> lockedStocks = sortedMenuIds.stream()
                .map(menuId -> stockRepository.findByMenuIdWithLock(menuId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_INSUFFICIENT)))
                .toList();

        // menuId → Stock 으로 꺼내 쓰기 위한 Map
        Map<Long, Stock> stockMap = lockedStocks.stream()
                .collect(Collectors.toMap(Stock::getMenuId, s -> s));

        // 6. 재고 차감 + 총 결제 금액 계산
        //    menu.getPrice() 를 쓰므로 클라이언트가 보낸 금액은 일절 신뢰하지 않음
        long totalAmount = 0L;
        for (OrderRequest.OrderItemRequest itemReq : sortedItems) {
            Menu menu = menuMap.get(itemReq.menuId());
            Stock stock = stockMap.get(itemReq.menuId());
            stock.decrease(itemReq.quantity()); // 재고 부족 시 STOCK_INSUFFICIENT 예외
            totalAmount += menu.getPrice() * itemReq.quantity();
        }

        // 7. 포인트 비관적 락 + 차감
        UserPoint userPoint = userPointRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        userPoint.use(totalAmount); // 잔액 부족 시 POINT_INSUFFICIENT 예외

        // 8. 주문 저장 — orderId가 필요하므로 먼저 저장
        Order order = Order.create(userId, totalAmount);
        orderRepository.save(order);

        // 9. 주문 아이템 생성 및 저장
        //    menuName, unitPrice 는 주문 시점 스냅샷으로 OrderItem 안에 저장됨
        List<OrderItem> orderItems = sortedItems.stream()
                .map(itemReq -> {
                    Menu menu = menuMap.get(itemReq.menuId());
                    return OrderItem.create(
                            order,
                            itemReq.menuId(),
                            menu.getName(),
                            menu.getPrice(),
                            itemReq.quantity()
                    );
                })
                .toList();
        orderItemRepository.saveAll(orderItems);

        // 10. 포인트 사용 내역 저장 — order.getId() 는 8번 저장 후에야 존재
        PointHistory history = PointHistory.ofUse(userId, totalAmount, userPoint.getBalance(), order.getId());
        pointHistoryRepository.save(history);

        // 11. Spring 이벤트 발행 — 트랜잭션 커밋 후 OrderKafkaProducer.publish()가 실행됨
        //     orderItems에서 menuId, quantity만 추출해 이벤트 객체로 변환
        List<OrderCompletedEvent.OrderItemInfo> eventItems = orderItems.stream()
                .map(item -> new OrderCompletedEvent.OrderItemInfo(item.getMenuId(), item.getQuantity()))
                .toList();
        eventPublisher.publishEvent(new OrderCompletedEvent(eventItems));

        return OrderResponse.from(order, orderItems);
    }
}
