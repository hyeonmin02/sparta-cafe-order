package org.example.spartacafe.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // AUTH (A)
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "A001", "이미 사용 중인 아이디입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A002", "아이디 또는 비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A003", "인증이 필요합니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "A004", "토큰이 만료되었습니다."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "A005", "유효하지 않은 토큰입니다."),

    // USER (U)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "존재하지 않는 회원입니다."),

    // MENU (M)
    MENU_NOT_FOUND(HttpStatus.BAD_REQUEST, "M001", "존재하지 않는 메뉴입니다."),
    MENU_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "M002", "판매 중지된 메뉴입니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "M003", "존재하지 않는 카테고리입니다."),

    // STOCK (S)
    STOCK_INSUFFICIENT(HttpStatus.UNPROCESSABLE_ENTITY, "S001", "재고가 부족한 메뉴가 있습니다."),

    // POINT (P)
    INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "P001", "충전 금액이 올바르지 않습니다."),
    AMOUNT_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "P002", "충전 한도를 초과했습니다."),
    POINT_INSUFFICIENT(HttpStatus.UNPROCESSABLE_ENTITY, "P003", "포인트 잔액이 부족합니다."),

    // ORDER (O)
    ORDER_ITEMS_EMPTY(HttpStatus.BAD_REQUEST, "O001", "주문 항목이 비어있습니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "O002", "존재하지 않는 주문입니다."),

    // LOCK (L)
    LOCK_TIMEOUT(HttpStatus.CONFLICT, "L001", "처리 중인 요청이 있습니다. 잠시 후 다시 시도해주세요."),
    LOCK_INTERRUPTED(HttpStatus.INTERNAL_SERVER_ERROR, "L002", "처리 중 중단되었습니다."),

    // GLOBAL (G)
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "G001", "입력값이 올바르지 않습니다."),
    INVALID_FORMAT(HttpStatus.BAD_REQUEST, "G002", "요청 형식이 올바르지 않습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "G003", "요청한 리소스를 찾을 수 없습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "G004", "접근 권한이 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "G005", "서버 오류가 발생했습니다."),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "G006", "일시적으로 서비스를 이용할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
