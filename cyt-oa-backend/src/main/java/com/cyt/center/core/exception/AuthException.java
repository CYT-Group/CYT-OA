package com.cyt.center.core.exception;

/**
 * 身份或权限不满足：401 未登录、403 无权限。与业务异常分开，前端据此决定是跳登录还是提示。
 */
public class AuthException extends RuntimeException {

    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;

    private final int code;

    public AuthException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static AuthException unauthorized(String message) {
        return new AuthException(UNAUTHORIZED, message);
    }

    public static AuthException forbidden(String message) {
        return new AuthException(FORBIDDEN, message);
    }

}
