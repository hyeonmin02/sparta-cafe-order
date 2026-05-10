package org.example.spartacafe.domain.stock.repository;

import jakarta.persistence.LockModeType;
import org.example.spartacafe.domain.stock.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByMenuId(Long menuId);

    // 재고 관련 동시성 제어 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.menuId = :menuId")
    Optional<Stock> findByMenuIdWithLock(Long menuId);
}
