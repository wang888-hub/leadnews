package com.aaliyun.leadnews.wemedia.web;

import com.aaliyun.leadnews.common.api.CommonErrorCode;
import com.aaliyun.leadnews.common.api.ResponseResult;
import com.aaliyun.leadnews.common.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = WemediaController.class)
public class ContinuationExceptionHandler {
    private final ObjectMapper objectMapper;

    public ContinuationExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @ExceptionHandler(value = BusinessException.class, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    ResponseEntity<String> business(BusinessException exception) {
        HttpStatus status = switch (exception.getCode() / 100) {
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
        return error(status, exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(
            value = {MethodArgumentNotValidException.class, ConstraintViolationException.class,
                    MethodArgumentTypeMismatchException.class},
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    ResponseEntity<String> validation(Exception ignored) {
        return error(HttpStatus.BAD_REQUEST, CommonErrorCode.VALIDATION_FAILED.code(),
                CommonErrorCode.VALIDATION_FAILED.message());
    }

    private ResponseEntity<String> error(HttpStatus status, int code, String message) {
        try {
            String json = objectMapper.writeValueAsString(ResponseResult.error(code, message));
            return ResponseEntity.status(status).contentType(MediaType.TEXT_EVENT_STREAM)
                    .body("event: error\ndata: " + json + "\n\n");
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize continuation error", exception);
        }
    }
}
