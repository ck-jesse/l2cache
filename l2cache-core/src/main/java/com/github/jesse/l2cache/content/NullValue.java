package com.github.jesse.l2cache.content;

import java.io.Serializable;

/**
 * 简单的可序列化类，用作{@code null}替换缓存存储，否则不支持{@code null}值
 *
 * @author chenck
 * @date 2020/7/1 17:39
 */
public class NullValue implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final Object INSTANCE = new NullValue();

    private NullValue() {
    }

    private Object readResolve() {
        return INSTANCE;
    }

    @Override
    public boolean equals(Object obj) {
        return (this == obj || obj == null);
    }

    @Override
    public int hashCode() {
        return NullValue.class.hashCode();
    }

    @Override
    public String toString() {
        return "NullValue";
    }
}
