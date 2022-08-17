package com.github.jesse.l2cache.util;

import com.github.jesse.l2cache.content.NullValue;

/**
 * NullValue 工具类
 *
 * @author chenck
 * @date 2020/9/21 14:37
 */
public class NullValueUtil {

    /**
     * 转换为存储值
     */
    public static Object toStoreValue(Object userValue, boolean allowNullValues, String cacheName) {
        if (userValue == null) {
            if (allowNullValues) {
                return NullValue.INSTANCE;
            }
            throw new IllegalArgumentException("Cache '" + cacheName + "' is configured to not allow null values but null was provided");
        }
        return userValue;
    }

    /**
     * 从存储值解析为具体值
     */
    public static Object fromStoreValue(Object storeValue, boolean allowNullValues) {
        if (allowNullValues && storeValue instanceof NullValue) {
            return null;
        }
        return storeValue;
    }
}
