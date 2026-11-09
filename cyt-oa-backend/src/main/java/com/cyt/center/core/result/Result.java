package com.cyt.center.core.result;

import com.cyt.center.core.exception.BizException;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 统一响应体。code = 0 表示成功，非 0 表示失败。
 *
 * 除了 record 访问器，还保留 Lombok 风格的 getXxx()：移植过来的 ruoyi 代码按
 * getCode/getMsg/getData 读结果，前端旧代码按 data.msg 取提示文案，
 * message 与 msg 是同一份内容的两个字段。
 */
public record Result<T>(int code, String message, T data) {

    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "ok", data);
    }

    public static Result<Void> ok() {
        return new Result<>(0, "ok", null);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    /** 以下是从 ruoyi 移植过来的代码沿用的上游工厂方法名，语义与 ok/fail 完全相同。 */
    public static <T> Result<T> success(T data) {
        return ok(data);
    }

    public static Result<Void> success() {
        return ok();
    }

    public static <T> Result<T> error(Integer code, String message) {
        return fail(code == null ? 500 : code, message);
    }

    public static boolean isSuccess(Integer code) {
        return code != null && code == 0;
    }

    public String getMsg() {
        return message;
    }

    public T getData() {
        return data;
    }

    @JsonIgnore
    public boolean isSuccess() {
        return code == 0;
    }

    @JsonIgnore
    public void checkError() {
        if (!isSuccess()) {
            throw new BizException(message);
        }
    }

    /** 远程调用返回值拆包用这个：失败直接抛业务异常。 */
    @JsonIgnore // 避免 jackson 序列化：失败时 checkError 会抛异常导致整个响应写不出去
    public T getCheckedData() {
        checkError();
        return data;
    }
}
