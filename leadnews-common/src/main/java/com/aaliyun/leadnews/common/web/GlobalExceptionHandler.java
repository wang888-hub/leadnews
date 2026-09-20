package com.aaliyun.leadnews.common.web;

import com.aaliyun.leadnews.common.api.CommonErrorCode;
import com.aaliyun.leadnews.common.api.ResponseResult;
import com.aaliyun.leadnews.common.exception.BusinessException;
import feign.FeignException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ResponseResult<Void>> business(BusinessException ex) {
        HttpStatus status = switch (ex.getCode() / 100) {
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            case 409 -> HttpStatus.CONFLICT;
            case 429 -> HttpStatus.TOO_MANY_REQUESTS;
            case 502 -> HttpStatus.BAD_GATEWAY;
            case 503 -> HttpStatus.SERVICE_UNAVAILABLE;
            case 504 -> HttpStatus.GATEWAY_TIMEOUT;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(ResponseResult.error(ex.getCode(), ex.getMessage()));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    ResponseEntity<ResponseResult<Void>> validation(Exception ex) {
        return ResponseEntity.badRequest().body(ResponseResult.error(CommonErrorCode.VALIDATION_FAILED));
    }
    @ExceptionHandler(FeignException.class)
    ResponseEntity<ResponseResult<Void>> feign(FeignException ex) {
        log.error("Feign call failed", ex);
        if (ex.status() == 409) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ResponseResult.error(40900, "Downstream resource state changed concurrently"));
        }
        if (ex.status() == 403) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseResult.error(CommonErrorCode.AUTH_FORBIDDEN));
        }
        if (ex.status() == 404) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseResult.error(40400, "Downstream resource not found"));
        }
        return ResponseEntity.status(502).body(ResponseResult.error(CommonErrorCode.FEIGN_CALL_FAILED));
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ResponseResult<Void>> unknown(Exception ex) {
        log.error("Unhandled request error", ex);
        return ResponseEntity.internalServerError().body(ResponseResult.error(CommonErrorCode.SYSTEM_ERROR));
    }
}
