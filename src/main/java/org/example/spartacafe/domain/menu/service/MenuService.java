package org.example.spartacafe.domain.menu.service;

import lombok.RequiredArgsConstructor;
import org.example.spartacafe.domain.menu.dto.MenuResponse;
import org.example.spartacafe.domain.menu.dto.PopularMenuResponse;
import org.example.spartacafe.domain.menu.entity.Menu;
import org.example.spartacafe.domain.menu.enums.MenuStatus;
import org.example.spartacafe.domain.menu.repository.MenuRepository;
import org.example.spartacafe.domain.stock.entity.Stock;
import org.example.spartacafe.domain.stock.repository.StockRepository;
import org.example.spartacafe.global.exception.BusinessException;
import org.example.spartacafe.global.exception.ErrorCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService {

    // OrderKafkaConsumer와 동일한 키 사용
    private static final String POPULAR_MENU_KEY = "menu:popular";
    private static final int TOP_COUNT = 3;

    private final MenuRepository menuRepository;
    private final StockRepository stockRepository;
    private final StringRedisTemplate redisTemplate;

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

    public List<PopularMenuResponse> getPopularMenus() {
        // Redis ZSET에서 점수(주문 수량 합계) 높은 순으로 상위 3개 조회
        // reverseRangeWithScores: 점수 내림차순, 인덱스 0~2 = 3개
        Set<ZSetOperations.TypedTuple<String>> topMenus =
                redisTemplate.opsForZSet().reverseRangeWithScores(POPULAR_MENU_KEY, 0, TOP_COUNT - 1);

        // 주문 데이터가 없으면 빈 리스트 반환
        if (topMenus == null || topMenus.isEmpty()) {
            return List.of();
        }

        // Redis에서 꺼낸 menuId(String)를 Long으로 변환
        List<Long> menuIds = topMenus.stream()
                .map(tuple -> Long.parseLong(tuple.getValue()))
                .toList();

        // menuId 목록으로 DB에서 메뉴 정보 일괄 조회
        // menuId → Menu 맵으로 만들어 순위 루프에서 빠르게 꺼내 씀
        Map<Long, Menu> menuMap = menuRepository.findAllById(menuIds).stream()
                .collect(Collectors.toMap(Menu::getId, m -> m));

        // ZSET 결과는 점수 내림차순으로 정렬되어 있으므로 순서대로 순위 부여
        List<PopularMenuResponse> result = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : topMenus) {
            Long menuId = Long.parseLong(tuple.getValue());
            Menu menu = menuMap.get(menuId);
            if (menu != null) {
                result.add(PopularMenuResponse.of(rank++, menu, tuple.getScore().intValue()));
            }
        }
        return result;
    }
}
