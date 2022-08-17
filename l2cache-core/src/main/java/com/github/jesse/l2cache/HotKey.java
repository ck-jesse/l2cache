package com.github.jesse.l2cache;

import com.github.jesse.l2cache.spi.SPI;

import java.io.Serializable;
import java.util.function.Function;

/**
 * 热key自动识别
 *
 * @author zengjucai
 * @date 2021/6/10 13:45
 */
@SPI
public interface HotKey extends Serializable {

    /**
     * 是否为热key
     */
    <K> boolean ifHotKey(K key, Function<K, Object> cacheKeyBuilder);

}
