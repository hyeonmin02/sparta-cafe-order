-- =============================================
-- V1: 전체 테이블 초기 생성
-- FK 참조 순서: category → menus → stocks
--              users → user_point
--              users → orders → order_items
--              users → point_histories
-- =============================================

-- 카테고리 (메뉴의 상위 분류 — 음료, 베이커리 등)
CREATE TABLE category (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    category_name VARCHAR(50) NOT NULL,
    display_order INT         NOT NULL,
    PRIMARY KEY (id)
);

-- 회원
CREATE TABLE users (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    login_id  VARCHAR(50)  NOT NULL UNIQUE,
    password  VARCHAR(255) NOT NULL,
    user_role VARCHAR(20)  NOT NULL,
    PRIMARY KEY (id)
);

-- 메뉴 (category 테이블 참조)
CREATE TABLE menus (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    category_id BIGINT       NOT NULL,
    name        VARCHAR(100) NOT NULL,
    price       BIGINT       NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    created_at  DATETIME(6),
    updated_at  DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_menus_category FOREIGN KEY (category_id) REFERENCES category (id)
);

-- 재고 (메뉴 1개당 재고 1개 — menu_id UNIQUE)
CREATE TABLE stocks (
    id       BIGINT NOT NULL AUTO_INCREMENT,
    menu_id  BIGINT NOT NULL UNIQUE,
    quantity INT    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_stocks_menu FOREIGN KEY (menu_id) REFERENCES menus (id)
);

-- 유저 포인트 (user_id가 PK이자 FK — 회원 1명당 포인트 계좌 1개)
CREATE TABLE user_point (
    user_id    BIGINT NOT NULL,
    balance    BIGINT NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (user_id),
    CONSTRAINT fk_user_point_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- 주문
CREATE TABLE orders (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    user_id      BIGINT      NOT NULL,
    status       VARCHAR(20) NOT NULL,
    total_amount BIGINT      NOT NULL,
    paid_at      DATETIME(6),
    created_at   DATETIME(6),
    updated_at   DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- 주문 아이템 (주문 1개에 여러 아이템 — orders 테이블 참조)
-- menu_name, unit_price 는 주문 당시 스냅샷으로 저장 (메뉴 정보가 바뀌어도 주문 내역은 보존)
CREATE TABLE order_items (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    order_id   BIGINT       NOT NULL,
    menu_id    BIGINT       NOT NULL,
    menu_name  VARCHAR(100) NOT NULL,
    unit_price BIGINT       NOT NULL,
    quantity   INT          NOT NULL,
    sub_total  BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

-- 포인트 내역 (충전/사용 이력)
-- related_order_id: 사용 시 주문 ID, 충전 시 NULL
CREATE TABLE point_histories (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    user_id          BIGINT      NOT NULL,
    type             VARCHAR(20) NOT NULL,
    amount           BIGINT      NOT NULL,
    balance_after    BIGINT      NOT NULL,
    related_order_id BIGINT,
    created_at       DATETIME(6),
    updated_at       DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_point_histories_user FOREIGN KEY (user_id) REFERENCES users (id)
);
