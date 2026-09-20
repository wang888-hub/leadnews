package com.aaliyun.leadnews.common.exception;

import com.aaliyun.leadnews.common.api.ErrorCode;

public class BusinessException extends RuntimeException {
    private final int code;
    public BusinessException(ErrorCode errorCode) { super(errorCode.message()); this.code = errorCode.code(); }
    public BusinessException(int code, String message) { super(message); this.code = code; }
    public int getCode() { return code; }
}
