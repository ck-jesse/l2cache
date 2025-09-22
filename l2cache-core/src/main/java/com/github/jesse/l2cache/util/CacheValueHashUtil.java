package com.github.jesse.l2cache.util;

import cn.hutool.crypto.digest.MD5;
import com.github.jesse.l2cache.content.NullValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 缓存值哈希工具类
 *
 * @author chenck
 * @date 2025/1/19
 */
public class CacheValueHashUtil {

    private static final Logger logger = LoggerFactory.getLogger(CacheValueHashUtil.class);

    /**
     * 计算缓存值的MD5哈希
     * <p>
     * 作用：MD5哈希防重: 基于缓存值的MD5计算，避免相同内容的重复消息
     *
     * @param value 缓存值
     * @return MD5哈希字符串，如果值为null返回固定的"null"标识
     */
    public static String calcHash(Object value) {
        try {
            if (value == null) {
                return "null";
            }
            if (value instanceof NullValue) {
                return "nullvalue";
            }

            return MD5.create().digestHex(value.toString());
        } catch (Exception e) {
            logger.warn("calculate cache value hash error, return default hash", e);
            return "error";
        }
    }

}