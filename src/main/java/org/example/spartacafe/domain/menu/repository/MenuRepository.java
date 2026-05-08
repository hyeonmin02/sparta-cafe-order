package org.example.spartacafe.domain.menu.repository;

import org.example.spartacafe.domain.menu.entity.Menu;
import org.example.spartacafe.domain.menu.enums.MenuStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuRepository extends JpaRepository<Menu, Long> {
    List<Menu> findByCategoryIdAndStatus(Long categoryId, MenuStatus menuStatus);

    List<Menu> findByStatus(MenuStatus menuStatus);
}
