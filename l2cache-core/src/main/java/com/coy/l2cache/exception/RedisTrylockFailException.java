package com.coy.l2cache.exception;

/**
 * @author chenck
 * @date 2020/9/22 20:26
 */
public class RedisTrylockFailException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private int code;

    public RedisTrylockFailException() {
    }

    public RedisTrylockFailException(String message) {
        super(message);
    }

    public RedisTrylockFailException(int code, String message) {
        super(message);
        this.code = code;
    }


    public int getCode() {
        return this.code;
    }
}
