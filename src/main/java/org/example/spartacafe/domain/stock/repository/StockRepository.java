package org.example.spartacafe.domain.stock.repository;

import org.example.spartacafe.domain.stock.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByMenuId(Long menuId);
}
