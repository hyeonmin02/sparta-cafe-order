# 06. API 명세서

> **버전**: v1.1
> **연관 문서**: 05. 기능 명세서, 07. 에러코드 설계서

---

## 1. 공통 사항

### 1-1. Base URL

| 환경 | URL |
|------|-----|
| 로컬 | `http://localhost:8080` |

> 모든 엔드포인트는 `/api` prefix 를 포함한다. 예) `POST /api/auth/signup`

### 1-2. 인증 방식

`Authorization: Bearer {accessToken}` 헤더로 JWT 전달.

userId 는 토큰의 `sub` claim 에서만 추출하며, 요청 본문/쿼리에 userId 가 있어도 무시한다.

### 1-3. 공통 응답 구조

**성공 응답**:
```json
{
  "data": { ... }
}
```

**에러 응답**:
```json
{
  "code": "P003",
  "message": "포인트 잔액이 부족합니다.",
  "details": {
    "required": 10000,
    "current": 7500,
    "shortage": 2500
  }
}
```

> 에러 응답 코드 체계 및 전체 목록은 [07. 에러코드 설계서](./07_에러코드_설계서.md) 참조.

### 1-4. 공통 헤더

| 헤더 | 필수 여부 | 설명 |
|------|-----------|------|
| Content-Type | 필수 (POST) | `application/json` |
| Authorization | 인증 필요 API 만 | `Bearer {token}` |

---

## 2. API 목록

| # | 메서드 | 경로 | 설명 | 인증 |
|---|--------|------|------|------|
| 1 | POST | `/api/auth/signup` | 회원가입 | ❌ |
| 2 | POST | `/api/auth/login` | 로그인 (JWT 발급) | ❌ |
| 3 | GET | `/api/menus` | 메뉴 목록 조회 | ❌ |
| 4 | GET | `/api/menus/popular` | 인기 메뉴 Top 3 조회 | ❌ |
| 5 | POST | `/api/points/charge` | 포인트 충전 | ✅ |
| 6 | GET | `/api/points/balance` | 포인트 잔액 조회 | ✅ |
| 7 | GET | `/api/points/history` | 포인트 충전/사용 내역 조회 | ✅ |
| 8 | POST | `/api/orders` | 주문/결제 | ✅ |

---

## 3. 인증

### 3-1. POST /api/auth/signup

**요청**:
```json
{
  "loginId": "kim_jiyeon",
  "password": "MyPass1234"
}
```

**성공 응답** (201 Created):
```json
{
  "data": {
    "id": 1
  }
}
```

**에러**:

| HTTP | code | 사유 |
|------|------|------|
| 400 | G001 INVALID_REQUEST | 유효성 위반 |
| 409 | A001 DUPLICATE_LOGIN_ID | loginId 중복 |

---

### 3-2. POST /api/auth/login

**요청**:
```json
{
  "loginId": "kim_jiyeon",
  "password": "MyPass1234"
}
```

**성공 응답** (200 OK):
```json
{
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 1800
  }
}
```

**에러**:

| HTTP | code | 사유 |
|------|------|------|
| 401 | A002 INVALID_CREDENTIALS | loginId/password 불일치 |

---

## 4. 메뉴

### 4-1. GET /api/menus

**쿼리 파라미터**:

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| categoryId | Long | 선택 | 카테고리 필터 |

**성공 응답** (200 OK):
```json
{
  "data": [
    {
      "id": 1,
      "name": "아메리카노",
      "price": 2500,
      "categoryName": "음료",
      "soldOut": false
    },
    {
      "id": 2,
      "name": "카페라떼",
      "price": 3000,
      "categoryName": "음료",
      "soldOut": true
    }
  ]
}
```

---

### 4-2. GET /api/menus/popular

**성공 응답** (200 OK):
```json
{
  "data": [
    {
      "rank": 1,
      "menuId": 1,
      "name": "아메리카노",
      "price": 2500,
      "orderCount": 142,
      "windowDays": 7,
      "calculatedAt": "2026-05-11T14:30:00"
    },
    {
      "rank": 2,
      "menuId": 2,
      "name": "카페라떼",
      "price": 3000,
      "orderCount": 98,
      "windowDays": 7,
      "calculatedAt": "2026-05-11T14:30:00"
    },
    {
      "rank": 3,
      "menuId": 3,
      "name": "라임모히또",
      "price": 3800,
      "orderCount": 73,
      "windowDays": 7,
      "calculatedAt": "2026-05-11T14:30:00"
    }
  ]
}
```

> `windowDays`: 인기 집계 기준 기간 (7일 고정).
> `calculatedAt`: API 응답 생성 시각. Redis ZSET 은 주문 발생 시 실시간 갱신, 매일 03:00 DB 기준 보정.

---

## 5. 포인트

### 5-1. POST /api/points/charge

**인증 필요**

**요청**:
```json
{
  "amount": 10000
}
```

> `amount`: 최소 100, 최대 1,000,000

**성공 응답** (201 Created):
```json
{
  "data": {
    "balance": 25000,
    "chargedAmount": 10000,
    "chargedAt": "2026-05-11T14:31:00"
  }
}
```

**에러**:

| HTTP | code | 사유 |
|------|------|------|
| 400 | P001 INVALID_AMOUNT | amount 유효성 위반 (100 미만 또는 1,000,000 초과) |
| 401 | A003 UNAUTHORIZED | JWT 만료/위조 |
| 409 | L001 LOCK_TIMEOUT | 분산락 대기 타임아웃 |

---

### 5-2. GET /api/points/balance

**인증 필요**

**성공 응답** (200 OK):
```json
{
  "data": {
    "userId": 1,
    "balance": 25000
  }
}
```

---

### 5-3. GET /api/points/history

**인증 필요**

**성공 응답** (200 OK):
```json
{
  "data": [
    {
      "id": 10,
      "type": "CHARGE",
      "amount": 10000,
      "balanceAfter": 25000,
      "relatedOrderId": null,
      "createdAt": "2026-05-11T14:31:00"
    },
    {
      "id": 9,
      "type": "USE",
      "amount": -7500,
      "balanceAfter": 15000,
      "relatedOrderId": 12345,
      "createdAt": "2026-05-11T13:20:00"
    }
  ]
}
```

> `type`: `CHARGE`(충전) 또는 `USE`(사용).
> `amount`: 충전은 양수, 사용은 음수.
> `relatedOrderId`: 사용 내역에만 존재, 충전 내역은 `null`.
> 최신순 정렬.

---

## 6. 주문/결제

### 6-1. POST /api/orders

**인증 필요**

**요청**:
```json
{
  "items": [
    { "menuId": 1, "quantity": 2 },
    { "menuId": 2, "quantity": 1 }
  ]
}
```

**성공 응답** (200 OK):
```json
{
  "data": {
    "orderId": 12345,
    "totalAmount": 8000,
    "items": [
      {
        "menuId": 1,
        "menuName": "아메리카노",
        "quantity": 2,
        "unitPrice": 2500,
        "subTotal": 5000
      },
      {
        "menuId": 2,
        "menuName": "카페라떼",
        "quantity": 1,
        "unitPrice": 3000,
        "subTotal": 3000
      }
    ],
    "paidAt": "2026-05-11T14:32:15"
  }
}
```

> 본 응답이 영수증 역할을 한다. `totalAmount` 는 클라이언트 전달 금액을 신뢰하지 않고 DB 메뉴 가격 기준으로 서버에서 계산한다.

**에러**:

| HTTP | code | 사유 | details |
|------|------|------|---------|
| 400 | O001 ORDER_ITEMS_EMPTY | items 비어있음 | — |
| 400 | G001 INVALID_REQUEST | quantity ≤ 0 등 유효성 위반 | field 별 사유 |
| 400 | M001 MENU_NOT_FOUND | 존재하지 않는 메뉴 | menuId |
| 400 | M002 MENU_NOT_AVAILABLE | INACTIVE 메뉴 | menuId |
| 401 | A003 UNAUTHORIZED | JWT 만료/위조 | — |
| 422 | S001 STOCK_INSUFFICIENT | 재고 부족 | menuId, available |
| 422 | P003 POINT_INSUFFICIENT | 포인트 부족 | required, current, shortage |

---

## 7. HTTP 상태 코드 정책

| 코드 | 사용 상황 |
|------|-----------|
| 200 | 조회 성공, 주문 생성 성공 |
| 201 | 회원가입, 포인트 충전 성공 |
| 400 | 입력값 유효성 위반, 존재하지 않는 리소스 참조 |
| 401 | 인증 실패 (토큰 부재/만료/위조) |
| 409 | 충돌 (중복, 락 타임아웃) |
| 422 | 비즈니스 규칙 위반 (잔액 부족, 재고 부족) |
| 500 | 서버 내부 오류 |

> **422 vs 409 사용 기준**: 422 는 비즈니스 규칙 위반(처리 불가능), 409 는 자원 충돌·동시성(재시도 가능). 잔액/재고 부족은 재시도해도 같은 결과이므로 422.

---

## 8. 페이지네이션 / 정렬 정책

본 시스템의 8개 API 중 페이지네이션이 필요한 API 는 없다 (메뉴 목록은 전체 반환, 인기 메뉴는 Top 3 고정, 포인트 내역은 전체 반환). 추후 주문 내역 조회 API 가 추가되면 Cursor 기반(`(created_at, id)`) 페이지네이션을 적용할 예정.

---

## 9. CORS 정책

| 항목 | 값 |
|------|----|
| Allowed Origins | `*` (개발 환경) |
| Allowed Methods | GET, POST |
| Allowed Headers | Authorization, Content-Type |

> 운영 환경 배포 시 클라이언트 도메인으로 한정 필요.
