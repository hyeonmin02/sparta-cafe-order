# 04. ERD — 데이터 모델 설계서

> **버전**: v1.0
> **연관 문서**: 03. 유스케이스 명세서, 05. 기능 명세서, 09. 동시성 제어 설계서

---

## 1. 모델링 핵심 원칙

| # | 원칙 | 결정 |
|---|------|------|
| 1 | 변경 빈도가 다른 데이터는 분리 | `user` ↔ `user_point`, `menu` ↔ `stock` |
| 2 | 포인트의 모든 변동은 이력으로 보존 | `point_history` (append-only) |
| 3 | 거래 시점의 가격은 스냅샷으로 보존 | `order_item.unit_price`, `order_item.menu_name` |
| 4 | 카테고리는 단일 레벨 | 부모-자식 자기참조 미사용 |
| 5 | 메뉴는 물리 삭제 대신 비활성화 | `menu.status = INACTIVE` (soft delete) |

---

## 2. ERD (Mermaid)

```mermaid
erDiagram
    USER ||--|| USER_POINT : "1:1"
    USER ||--o{ POINT_HISTORY : "1:N"
    USER ||--o{ ORDERS : "1:N"

    CATEGORY ||--o{ MENU : "1:N"
    MENU ||--|| STOCK : "1:1"
    MENU ||--o{ ORDER_ITEM : "1:N"

    ORDERS ||--|{ ORDER_ITEM : "1:N"
    ORDERS ||--o{ POINT_HISTORY : "1:N (USE 이력)"

    USER {
        BIGINT id PK
        VARCHAR login_id UK
        VARCHAR password
        VARCHAR role
        TIMESTAMP created_at
    }

    USER_POINT {
        BIGINT user_id PK_FK
        BIGINT balance
        BIGINT version
        TIMESTAMP updated_at
    }

    POINT_HISTORY {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR type "CHARGE / USE"
        BIGINT amount "양수=충전, 음수=사용"
        BIGINT balance_after
        BIGINT related_order_id "nullable"
        TIMESTAMP created_at
    }

    CATEGORY {
        BIGINT id PK
        VARCHAR name
        INT display_order
    }

    MENU {
        BIGINT id PK
        BIGINT category_id FK
        VARCHAR name
        BIGINT price
        VARCHAR status "ACTIVE / INACTIVE"
        TIMESTAMP created_at
    }

    STOCK {
        BIGINT menu_id PK_FK
        INT quantity
        BIGINT version
        TIMESTAMP updated_at
    }

    ORDERS {
        BIGINT id PK
        BIGINT user_id FK
        BIGINT total_amount
        VARCHAR status "CREATED / PAID"
        TIMESTAMP created_at
        TIMESTAMP paid_at
    }

    ORDER_ITEM {
        BIGINT id PK
        BIGINT order_id FK
        BIGINT menu_id FK
        VARCHAR menu_name "스냅샷"
        INT quantity
        BIGINT unit_price "스냅샷"
        BIGINT subtotal
    }
```

---

## 3. 테이블 명세

### 3-1. USER — 회원 마스터

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 시스템 내부 식별자, JWT subject |
| login_id | VARCHAR(50) | UNIQUE, NOT NULL | 로그인 식별자 |
| password | VARCHAR(255) | NOT NULL | BCrypt 해시 |
| role | VARCHAR(20) | NOT NULL, DEFAULT 'USER' | 역할 (현재 USER 단일) |
| created_at | TIMESTAMP | NOT NULL | 가입 시각 |

> **포인트 잔액을 두지 않는 이유**: 잔액은 트랜잭션마다 갱신되며 락이 필요한데, 같은 테이블에 두면 마스터 정보 조회까지 락에 영향. → `user_point` 분리.

---

### 3-2. USER_POINT — 포인트 잔액

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| user_id | BIGINT | PK, FK → user.id | 사용자 1명당 1행 |
| balance | BIGINT | NOT NULL, DEFAULT 0 | 현재 잔액 (캐시 성격) |
| version | BIGINT | NOT NULL, DEFAULT 0 | 낙관적 락 확장 대비 |
| updated_at | TIMESTAMP | NOT NULL | 최종 갱신 시각 |

> **balance 는 캐시, 진실의 원천은 `point_history`**. 두 값이 어긋나면 이력 합산이 정답이다.

---

### 3-3. POINT_HISTORY — 포인트 이력 (append-only)

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| user_id | BIGINT | NOT NULL, FK → user.id | |
| type | VARCHAR(20) | NOT NULL | `CHARGE` / `USE` |
| amount | BIGINT | NOT NULL | 양수=충전, 음수=사용 |
| balance_after | BIGINT | NOT NULL | 변동 직후 잔액 (가계부 형식) |
| related_order_id | BIGINT | NULL, FK → orders.id | USE 일 때만 채움 |
| created_at | TIMESTAMP | NOT NULL | |

> **append-only**: 이 테이블은 절대 UPDATE / DELETE 하지 않는다. 잔액 검증과 감사(audit) 데이터의 진실성을 보장한다.

---

### 3-4. CATEGORY — 메뉴 카테고리

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK | |
| name | VARCHAR(50) | NOT NULL | "에스프레소", "디카페인" 등 |
| display_order | INT | NOT NULL, DEFAULT 0 | 클라이언트 노출 순서 |

> **단일 레벨 정책**: 부모-자식 구조 미도입. 카페 도메인에서 계층 카테고리는 과잉 설계.

---

### 3-5. MENU — 메뉴 정보

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK | |
| category_id | BIGINT | NOT NULL, FK → category.id | |
| name | VARCHAR(100) | NOT NULL | |
| price | BIGINT | NOT NULL | 원 단위 정수 |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' | `ACTIVE` / `INACTIVE` |
| created_at | TIMESTAMP | NOT NULL | |

> **price 를 BIGINT 로 두는 이유**: 원 단위 정수면 충분하며 부동소수점 정밀도 문제를 회피.
> **soft delete**: 과거 주문이 메뉴를 참조하므로 물리 삭제 시 영수증 조회 불가능. INACTIVE 처리.

---

### 3-6. STOCK — 메뉴 재고

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| menu_id | BIGINT | PK, FK → menu.id | 1:1 관계 |
| quantity | INT | NOT NULL, DEFAULT 0 | 현재 판매 가능 수량 |
| version | BIGINT | NOT NULL, DEFAULT 0 | 낙관적 락 확장 대비 |
| updated_at | TIMESTAMP | NOT NULL | |

> **MENU 와 분리한 이유**: USER ↔ USER_POINT 와 동일. 재고는 락 대상, 메뉴 정보는 락 무관.
> **가점유 컬럼(reserved_quantity) 미도입**: 포인트 즉시 결제이므로 가점유 상태가 길어질 일 없음. 단순 차감-실패시-복구 패턴으로 충분. → ADR-005 참조.

---

### 3-7. ORDERS — 주문 본체

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| user_id | BIGINT | NOT NULL, FK → user.id | |
| total_amount | BIGINT | NOT NULL | 항목 소계 합산 (스냅샷) |
| status | VARCHAR(20) | NOT NULL | `CREATED` / `PAID` |
| created_at | TIMESTAMP | NOT NULL | 주문 생성 시각 (인기 메뉴 집계 기준) |
| paid_at | TIMESTAMP | NULL | 결제 확정 시각 |

> **status 2단계로 두는 이유**: 단일 트랜잭션 내에서 두 상태가 거의 동시에 발생하지만, "재고는 잡았는데 결제 실패" 분기를 코드/데이터 양쪽에서 명시적으로 표현하기 위함. → 09. 동시성 설계서에서 분기 시나리오로 활용.

---

### 3-8. ORDER_ITEM — 주문 항목

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| order_id | BIGINT | NOT NULL, FK → orders.id | |
| menu_id | BIGINT | NOT NULL, FK → menu.id | |
| menu_name | VARCHAR(100) | NOT NULL | 주문 시점 이름 (스냅샷) |
| quantity | INT | NOT NULL | |
| unit_price | BIGINT | NOT NULL | 주문 시점 단가 (스냅샷) |
| subtotal | BIGINT | NOT NULL | unit_price × quantity |

> **스냅샷 컬럼(menu_name, unit_price)**: 메뉴 가격/이름이 추후 변경되어도 과거 주문 영수증 금액은 불변이어야 한다. 커머스 시스템 표준 패턴.

---

## 4. 인덱스 전략

| 테이블 | 인덱스 | 목적 |
|--------|--------|------|
| user | `UK(login_id)` | 로그인 식별자 조회 + 중복 방지 |
| orders | `idx(created_at)` | 인기 메뉴 7일 윈도우 집계 |
| orders | `idx(user_id, created_at DESC)` | 사용자별 주문 이력 (선택) |
| order_item | `idx(order_id)` | 주문 상세 조회 |
| order_item | `idx(menu_id)` | 인기 메뉴 GROUP BY |
| point_history | `idx(user_id, created_at DESC)` | 사용자별 이력 시간 역순 조회 |

---

## 5. 시드 데이터 설계

> Flyway `V2__insert_seed.sql` 로 주입. 동시성 테스트 시나리오를 의식하여 구성.

| 카테고리 | 메뉴 | 가격 | 초기 재고 | 용도 |
|----------|------|------|-----------|------|
| 에스프레소 | 아메리카노 | 4,500 | 100 | 일반 주문 흐름 |
| 에스프레소 | 카페라떼 | 5,000 | 100 | 다중 메뉴 주문 |
| 디카페인 | 디카페인 아메리카노 | 5,000 | 50 | 카테고리 필터링 |
| 스무디 | 딸기 스무디 | 6,000 | 30 | 일반 |
| 주스 | 자몽 주스 | 5,500 | 20 | 일반 |
| 시그니처 | 오늘의 시그니처 | 7,000 | **5** | 한정 메뉴 동시 주문 시나리오 |
| 시그니처 | 스페셜 라떼 | 7,500 | **1** | 동시성 테스트 극단 케이스 |
| 디저트 | 마카롱 | 3,500 | 50 | 다중 카테고리 주문 |

> **재고 5/1 메뉴**: 09. 동시성 설계서의 C2 시나리오(여러 사용자가 마지막 한 잔을 두고 다투는 경우) 검증용.

---

## 6. 모델이 다루지 않는 것 (의도적 제외)

| 항목 | 사유 | 추후 도입 시 영향 |
|------|------|-------------------|
| 매장(store) | 단일 매장 가정 | menu/stock 에 store_id 추가 필요 |
| 주문 취소·환불 | 키오스크 모델은 직원 처리 | orders.status 확장 + REFUND 이력 |
| 가점유 재고 | 즉시 결제이므로 불필요 | stock 에 reserved_quantity 추가 |
| 멱등성 키 | 5일 일정 한계 | idempotency_key 테이블 신설 |
| Outbox 패턴 | AFTER_COMMIT 으로 충분 | outbox_event 테이블 신설 |

> 위 누락은 시스템 한계가 아닌 명시적 결정. ADR 에서 각 항목의 검토 근거를 별도 기록.