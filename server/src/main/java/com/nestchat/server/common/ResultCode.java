package com.nestchat.server.common;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(0, "success"),
    PARAM_ERROR(40001, "参数错误"),
    CAPTCHA_ERROR(40002, "验证码错误"),
    AUTH_ERROR(40003, "账号或密码错误"),
    CAPTCHA_EXPIRED(40004, "验证码已过期"),
    PASSWORD_MISMATCH(40005, "两次密码不一致"),
    UNAUTHORIZED(40101, "未登录或token失效"),
    FORBIDDEN(40301, "无权限操作"),
    NOT_FOUND(40401, "资源不存在"),
    DUPLICATE_REGISTRATION(40901, "重复注册"),
    BINDING_EXISTS(40902, "已存在绑定关系"),
    APPLICATION_EXISTS(40903, "绑定申请已存在"),
    SERVER_ERROR(50000, "服务端异常");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
