# 04. ERD — 데이터 모델 설계서

> **버전**: v1.1
> **연관 문서**: 03. 유스케이스 명세서, 05. 기능 명세서, 09. 동시성 제어 설계서

---

## 1. 모델링 핵심 원칙

| # | 원칙 | 결정 |
|---|------|------|
| 1 | 변경 빈도가 다른 데이터는 분리 | `users` ↔ `user_point`, `menus` ↔ `stocks` |
| 2 | 포인트의 모든 변동은 이력으로 보존 | `point_histories` (append-only) |
| 3 | 거래 시점의 가격은 스냅샷으로 보존 | `order_items.unit_price`, `order_items.menu_name` |
| 4 | 카테고리는 단일 레벨 | 부모-자식 자기참조 미사용 |
| 5 | 메뉴는 물리 삭제 대신 비활성화 | `menus.status = INACTIVE` (soft delete) |

---

## 2. ERD (Mermaid)

```mermaid
erDiagram
    USERS ||--|| USER_POINT : "1:1"
    USERS ||--o{ POINT_HISTORIES : "1:N"
    USERS ||--o{ ORDERS : "1:N"

    CATEGORY ||--o{ MENUS : "1:N"
    MENUS ||--|| STOCKS : "1:1 (UNIQUE)"
    MENUS ||--o{ ORDER_ITEMS : "1:N (참조용, DB FK 없음)"

    ORDERS ||--|{ ORDER_ITEMS : "1:N"

    USERS {
        BIGINT id PK
        VARCHAR login_id UK
        VARCHAR password
        VARCHAR user_role
    }

    USER_POINT {
        BIGINT user_id PK_FK
        BIGINT balance
        DATETIME created_at
        DATETIME updated_at
    }

    POINT_HISTORIES {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR type "CHARGE / USE"
        BIGINT amount "양수=충전, 음수=사용"
        BIGINT balance_after
        BIGINT related_order_id "nullable"
        DATETIME created_at
        DATETIME updated_at
    }

    CATEGORY {
        BIGINT id PK
        VARCHAR category_name
        INT display_order
    }

    MENUS {
        BIGINT id PK
        BIGINT category_id FK
        VARCHAR name
        BIGINT price
        VARCHAR status "ACTIVE / INACTIVE"
        DATETIME created_at
        DATETIME updated_at
    }

    STOCKS {
        BIGINT id PK
        BIGINT menu_id UK_FK
        INT quantity
    }

    ORDERS {
        BIGINT id PK
        BIGINT user_id FK
        BIGINT total_amount
        VARCHAR status "CREATED"
        DATETIME paid_at "nullable"
        DATETIME created_at
        DATETIME updated_at
    }

    ORDER_ITEMS {
        BIGINT id PK
        BIGINT order_id FK
        BIGINT menu_id "참조용 (DB FK 없음)"
        VARCHAR menu_name "스냅샷"
        INT quantity
        BIGINT unit_price "스냅샷"
        BIGINT sub_total
    }
```

---

## 3. 테이블 명세

### 3-1. USERS — 회원 마스터

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 시스템 내부 식별자, JWT subject |
| login_id | VARCHAR(50) | UNIQUE, NOT NULL | 로그인 식별자 |
| password | VARCHAR(255) | NOT NULL | BCrypt 해시 |
| user_role | VARCHAR(20) | NOT NULL | 역할 (현재 USER 단일) |

> **포인트 잔액을 두지 않는 이유**: 잔액은 트랜잭션마다 갱신되며 락이 필요한데, 같은 테이블에 두면 마스터 정보 조회까지 락에 영향. → `user_point` 분리.

---

### 3-2. USER_POINT — 포인트 잔액

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| user_id | BIGINT | PK, FK → users.id | 사용자 1명당 1행 |
| balance | BIGINT | NOT NULL | 현재 잔액 (캐시 성격) |
| created_at | DATETIME(6) | | JPA Auditing 자동 세팅 |
| updated_at | DATETIME(6) | | JPA Auditing 자동 세팅 |

> **balance 는 캐시, 진실의 원천은 `point_histories`**. 두 값이 어긋나면 이력 합산이 정답이다.
> **@Version(낙관적 락) 미사용**: 포인트 충전은 Redisson 분산락 + 비관적 락으로 제어. → 09. 동시성 설계서 참조.

---

### 3-3. POINT_HISTORIES — 포인트 이력 (append-only)

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| user_id | BIGINT | NOT NULL, FK → users.id | |
| type | VARCHAR(20) | NOT NULL | `CHARGE` / `USE` |
| amount | BIGINT | NOT NULL | 양수=충전, 음수=사용 |
| balance_after | BIGINT | NOT NULL | 변동 직후 잔액 (가계부 형식) |
| related_order_id | BIGINT | NULL | USE 일 때만 채움 (DB FK 없음, 참조용) |
| created_at | DATETIME(6) | | JPA Auditing 자동 세팅 |
| updated_at | DATETIME(6) | | JPA Auditing 자동 세팅 |

> **append-only**: 이 테이블은 절대 UPDATE / DELETE 하지 않는다. 잔액 검증과 감사(audit) 데이터의 진실성을 보장한다.

---

### 3-4. CATEGORY — 메뉴 카테고리

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| category_name | VARCHAR(50) | NOT NULL | "음료", "베이커리" 등 |
| display_order | INT | NOT NULL | 클라이언트 노출 순서 |

> **단일 레벨 정책**: 부모-자식 구조 미도입. 카페 도메인에서 계층 카테고리는 과잉 설계.

---

### 3-5. MENUS — 메뉴 정보

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| category_id | BIGINT | NOT NULL, FK → category.id | |
| name | VARCHAR(100) | NOT NULL | |
| price | BIGINT | NOT NULL | 원 단위 정수 |
| status | VARCHAR(20) | NOT NULL | `ACTIVE` / `INACTIVE` |
| created_at | DATETIME(6) | | JPA Auditing 자동 세팅 |
| updated_at | DATETIME(6) | | JPA Auditing 자동 세팅 |

> **price 를 BIGINT 로 두는 이유**: 원 단위 정수면 충분하며 부동소수점 정밀도 문제를 회피.
> **soft delete**: 과거 주문이 메뉴를 참조하므로 물리 삭제 시 영수증 조회 불가능. INACTIVE 처리.

---

### 3-6. STOCKS — 메뉴 재고

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| menu_id | BIGINT | NOT NULL, UNIQUE, FK → menus.id | 메뉴 1:1 |
| quantity | INT | NOT NULL | 현재 판매 가능 수량 |

> **MENUS 와 분리한 이유**: 재고는 비관적 락 대상, 메뉴 정보는 락 무관. 같은 테이블에 두면 메뉴 조회까지 락에 영향.
> **@Version(낙관적 락) 미사용**: 비관적 락(`SELECT FOR UPDATE`)으로 재고 차감 동시성 제어. → 09. 동시성 설계서 참조.

---

### 3-7. ORDERS — 주문 본체

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| user_id | BIGINT | NOT NULL, FK → users.id | |
| status | VARCHAR(20) | NOT NULL | `CREATED` |
| total_amount | BIGINT | NOT NULL | 항목 소계 합산 (서버 계산, 스냅샷) |
| paid_at | DATETIME(6) | NULL | 결제 확정 시각 (현재 미사용) |
| created_at | DATETIME(6) | | JPA Auditing 자동 세팅, 인기 메뉴 집계 기준 |
| updated_at | DATETIME(6) | | JPA Auditing 자동 세팅 |

---

### 3-8. ORDER_ITEMS — 주문 항목

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| order_id | BIGINT | NOT NULL, FK → orders.id | |
| menu_id | BIGINT | NOT NULL | 메뉴 참조용 (DB FK 미설정) |
| menu_name | VARCHAR(100) | NOT NULL | 주문 시점 이름 (스냅샷) |
| unit_price | BIGINT | NOT NULL | 주문 시점 단가 (스냅샷) |
| quantity | INT | NOT NULL | |
| sub_total | BIGINT | NOT NULL | unit_price × quantity |

> **스냅샷 컬럼(menu_name, unit_price)**: 메뉴 가격/이름이 추후 변경되어도 과거 주문 영수증 금액은 불변이어야 한다.
> **menu_id DB FK 미설정**: JPA 연관관계 없이 `Long menuId` 로만 참조. 인기 메뉴 집계 JPQL 쿼리에서 사용.

---

## 4. 인덱스 전략

| 테이블 | 인덱스 | 목적 |
|--------|--------|------|
| users | `UK(login_id)` | 로그인 식별자 조회 + 중복 방지 |
| stocks | `UK(menu_id)` | 메뉴당 재고 1행 보장 |
| orders | `idx(created_at)` | 인기 메뉴 7일 윈도우 집계 |
| order_items | `idx(order_id)` | 주문 상세 조회 |
| point_histories | `idx(user_id, created_at DESC)` | 사용자별 이력 시간 역순 조회 |

---

## 5. 시드 데이터 (V2__seed_data.sql)

> Flyway `V2__seed_data.sql` 로 주입. 카테고리·메뉴·재고 기본값만 삽입. 회원 데이터는 API 회원가입을 통해 생성.

| 카테고리 | 메뉴 | 가격 | 초기 재고 |
|----------|------|------|-----------|
| 음료 | 아메리카노 | 2,500 | 100 |
| 음료 | 카페라떼 | 3,000 | 100 |
| 음료 | 라임모히또 | 3,800 | 100 |
| 베이커리 | 초코 크루아상 | 6,500 | 100 |
| 베이커리 | 소금빵 | 3,000 | 100 |
| 디저트 | 버터떡 | 2,500 | 100 |

---

## 6. 모델이 다루지 않는 것 (의도적 제외)

| 항목 | 사유 | 추후 도입 시 영향 |
|------|------|-------------------|
| 매장(store) | 단일 매장 가정 | menus/stocks 에 store_id 추가 필요 |
| 주문 취소·환불 | 키오스크 모델은 직원 처리 | orders.status 확장 + REFUND 이력 |
| 가점유 재고 | 즉시 결제이므로 불필요 | stocks 에 reserved_quantity 추가 |
| 낙관적 락(@Version) | 비관적 락으로 충분 | entity 에 version 컬럼 추가 |
| Outbox 패턴 | AFTER_COMMIT 으로 충분 | outbox_event 테이블 신설 |

> 위 누락은 시스템 한계가 아닌 명시적 결정. ADR 에서 각 항목의 검토 근거를 별도 기록.
