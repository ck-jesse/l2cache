package com.github.l2cache.exception;

/**
 * @author chenck
 * @date 2021/1/27 18:42
 */
public class L2CacheException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private int code;

    public L2CacheException() {
    }

    public L2CacheException(String message) {
        super(message);
    }

    public L2CacheException(int code, String message) {
        super(message);
        this.code = code;
    }


    public int getCode() {
        return this.code;
    }
}
