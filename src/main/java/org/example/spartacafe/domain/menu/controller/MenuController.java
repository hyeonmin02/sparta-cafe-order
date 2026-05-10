package org.example.spartacafe.domain.menu.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.spartacafe.domain.menu.dto.MenuResponse;
import org.example.spartacafe.domain.menu.dto.PopularMenuResponse;
import org.example.spartacafe.domain.menu.service.MenuService;
import org.example.spartacafe.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "메뉴", description = "메뉴 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/menus")
public class MenuController {

    private final MenuService menuService;

    @Operation(summary = "메뉴 목록 조회", description = "카테고리 ID로 필터링할 수 있습니다. 미입력 시 전체 메뉴를 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<MenuResponse>>> getMenus(
            @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(ApiResponse.success(menuService.getMenus(categoryId)));
    }

    @Operation(summary = "인기 메뉴 TOP3 조회", description = "주문 수량 기준 상위 3개 메뉴를 반환합니다.")
    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<PopularMenuResponse>>> getPopularMenus() {
        return ResponseEntity.ok(ApiResponse.success(menuService.getPopularMenus()));
    }
}
