-- =============================================
-- V2: 초기 시드 데이터
-- 앱 구동에 필요한 기본 카테고리, 메뉴, 재고 삽입
-- 회원 데이터는 API 회원가입을 통해 생성하므로 포함하지 않음
-- =============================================

-- 카테고리
INSERT INTO category (category_name, display_order)
VALUES ('음료', 1),
       ('베이커리', 2),
       ('디저트', 3);

-- 메뉴 (category_id: 1=음료, 2=베이커리, 3=디저트)
INSERT INTO menus (category_id, name, price, status, created_at, updated_at)
VALUES (1, '아메리카노',  2500, 'ACTIVE', NOW(), NOW()),
       (1, '카페라떼',   3000, 'ACTIVE', NOW(), NOW()),
       (1, '라임모히또', 3800, 'ACTIVE', NOW(), NOW()),
       (2, '초코 크루아상',   6500, 'ACTIVE', NOW(), NOW()),
       (2, '소금빵',     3000, 'ACTIVE', NOW(), NOW()),
       (3, '버터떡', 2500, 'ACTIVE', NOW(), NOW());

-- 재고 (메뉴 ID 1~6, 각 100개)
-- AUTO_INCREMENT로 생성된 menus.id 순서와 일치
INSERT INTO stocks (menu_id, quantity)
VALUES (1, 100),
       (2, 100),
       (3, 100),
       (4, 100),
       (5, 100),
       (6, 100);
