package com.cyt.center.core.exception;

/**
 * 业务异常：可以被预期并直接展示给用户的错误。
 */
public class BizException extends RuntimeException {

    public BizException(String message) {
        super(message);
    }

}
