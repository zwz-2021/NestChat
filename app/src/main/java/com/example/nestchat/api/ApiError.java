package com.example.nestchat.api;

/**
 * 通用错误模型，占位使用。
 */
public class ApiError {

    public int code;
    public String message;
    public String traceId;

    public ApiError() {
    }

    public ApiError(int code, String message, String traceId) {
        this.code = code;
        this.message = message;
        this.traceId = traceId;
    }
}
