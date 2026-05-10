package org.example.spartacafe.domain.order.repository;

import org.example.spartacafe.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
