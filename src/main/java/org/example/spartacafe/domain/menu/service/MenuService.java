package org.example.spartacafe.domain.menu.service;

import lombok.RequiredArgsConstructor;
import org.example.spartacafe.domain.menu.dto.MenuResponse;
import org.example.spartacafe.domain.menu.entity.Menu;
import org.example.spartacafe.domain.menu.enums.MenuStatus;
import org.example.spartacafe.domain.menu.repository.MenuRepository;
import org.example.spartacafe.domain.stock.entity.Stock;
import org.example.spartacafe.domain.stock.repository.StockRepository;
import org.example.spartacafe.global.exception.BusinessException;
import org.example.spartacafe.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService {

    private final MenuRepository menuRepository;
    private final StockRepository stockRepository;

    public List<MenuResponse> getMenus(Long categoryId) {

        List<Menu> menus;
        // 카테고리 id가 있으면 카테고리 필터 통해서 조회하고
        if (categoryId != null) {
            menus = menuRepository.findByCategoryIdAndStatus(categoryId, MenuStatus.ACTIVE);
        } // 없으면 전체조회
        else {
            menus = menuRepository.findByStatus(MenuStatus.ACTIVE);
        }

        // 각 메뉴를 MenuResponse로 변환해서 리턴
        return menus.stream()
                .map(menu -> {
                    Stock stock = stockRepository.findByMenuId(menu.getId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));

                    // 재고 수량이 0이면 soldOut = true
                    boolean soldOut = stock.getQuantity() == 0;
                    return MenuResponse.from(menu, soldOut);
                })
                .toList();
    }
}
