package org.example.spartacafe.domain.order.repository;

import org.example.spartacafe.domain.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderQueryRepository extends JpaRepository<OrderItem, Long> {

    // 스케줄러 보정용 — 특정 시점 이후 메뉴별 주문 수량 합계를 내림차순으로 집계
    // 반환값: [menuId(Long), totalQuantity(Long)] 쌍의 배열 리스트
    // Object[]로 받는 이유: JPQL GROUP BY 집계 결과는 엔티티가 아닌 컬럼 값의 묶음이기 때문
    @Query("select oi.menuId, sum(oi.quantity) from OrderItem oi " +
           "join oi.order o " +
           "where o.createdAt >= :from " +
           "group by oi.menuId " +
           "order by sum(oi.quantity) desc")
    List<Object[]> sumQuantityGroupByMenuIdSince(@Param("from") LocalDateTime from);
}
