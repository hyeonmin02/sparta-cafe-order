package org.example.spartacafe.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.example.spartacafe.global.exception.ErrorCode;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        String message,
        Map<String, Object> details
) {
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static ErrorResponse of(ErrorCode errorCode, Map<String, Object> details) {
        return new ErrorResponse(
                errorCode.getCode(),
                errorCode.getMessage(),
                details == null || details.isEmpty() ? null : details
        );
    }
}
