package org.example.spartacafe.domain.menu.controller;

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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/menus")
public class MenuController {

    private final MenuService menuService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MenuResponse>>> getMenus(
            @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(ApiResponse.success(menuService.getMenus(categoryId)));
    }

    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<PopularMenuResponse>>> getPopularMenus() {
        return ResponseEntity.ok(ApiResponse.success(menuService.getPopularMenus()));
    }
}
